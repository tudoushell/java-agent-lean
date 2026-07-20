package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.dto.IndexResultDto;
import com.elliot.ai.rag.dto.KbDocumentDto;
import com.elliot.ai.rag.dto.ParsedText;
import com.elliot.ai.rag.dto.StoredFile;
import com.elliot.ai.rag.entity.DocumentChunk;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import com.elliot.ai.rag.enums.KnowledgeBaseStatus;
import com.elliot.ai.rag.factory.DocumentParseFactory;
import com.elliot.ai.rag.mapper.DocumentChunkMapper;
import com.elliot.ai.rag.mapper.KbDocumentMapper;
import com.elliot.ai.rag.mapper.KnowledgeBaseMapper;
import com.elliot.ai.rag.parse.AbstractDocumentParse;
import com.elliot.ai.rag.service.KbDocumentService;
import com.elliot.ai.rag.service.LocalFilesStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.abbreviate;

@RequiredArgsConstructor
@Service
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument>
        implements KbDocumentService {

    private final static String EMBEDDING_MODEL_NAME = "nomic-embed-text";
    private static final int INDEX_BATCH_SIZE = 10;


    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final LocalFilesStorageService localFilesStorageService;
    private final DocumentParseFactory documentParseFactory;
    private final VectorStore vectorStore;
    private final DocumentChunkMapper documentChunkMapper;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public IndexResultDto index(UUID documentId) {
        KbDocument kbDocument = getById(documentId);
        validateDocument(kbDocument, documentId);

        OffsetDateTime now = OffsetDateTime.now();
        kbDocument.setStatus(KbDocumentStatus.INDEXING);
        kbDocument.setEmbeddingModel(EMBEDDING_MODEL_NAME);
        kbDocument.setIndexedAt(now);
        updateById(kbDocument);
        //重新索引时，先删除文档原有向量
        try {
            vectorStore.delete("documentId == '" + documentId + "'");
            //构建写入向量数据库
            int vectorCount = indexByBatch(kbDocument);
            kbDocument.setVectorCount(vectorCount);
            kbDocument.setStatus(KbDocumentStatus.INDEXED);
            kbDocument.setIndexedAt(now);
            kbDocument.setEmbeddingModel(EMBEDDING_MODEL_NAME);
            updateById(kbDocument);
            return new IndexResultDto(documentId, vectorCount, EMBEDDING_MODEL_NAME, kbDocument.getStatus().getValue());
        } catch (Exception e) {
            deleteVectorsQuietly(documentId);
            kbDocument.setVectorCount(0);
            kbDocument.setStatus(KbDocumentStatus.FAILED);
            kbDocument.setErrorMessage(abbreviate(e.getMessage(), 200));
            updateById(kbDocument);
            throw new BusinessException(
                    ResultCode.FAIL,
                    "文档向量化失败：" + e.getMessage()
            );
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocumentDto upload(UUID knowledgeBaseId, MultipartFile file) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库不存在");
        }

        if (!KnowledgeBaseStatus.ENABLED.equals(knowledgeBase.getStatus())) {
            throw new BusinessException(ResultCode.FAIL, "知识库已停用，不能上传文档");
        }

        StoredFile storedFile = localFilesStorageService.store(file);
        if (existsSameFile(knowledgeBaseId, storedFile.sha256())) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "该知识库中已经存在相同内容的文档"
            );
        }

        KbDocument kbDocument = new KbDocument();
        kbDocument.setId(UUID.randomUUID());
        kbDocument.setKnowledgeBaseId(knowledgeBaseId);
        kbDocument.setOriginalName(storedFile.originalName());
        kbDocument.setStoredName(storedFile.storedName());
        kbDocument.setStoragePath(storedFile.relativePath());
        kbDocument.setContentType(storedFile.contentType());
        kbDocument.setFileExtension(storedFile.extension());
        kbDocument.setSizeBytes(storedFile.sizeBytes());
        kbDocument.setSha256(storedFile.sha256());
        kbDocument.setStatus(KbDocumentStatus.UPLOADED);

        if (!this.save(kbDocument)) {
            throw new BusinessException(ResultCode.FAIL, "文档记录创建失败");
        }
        try {
            AbstractDocumentParse parse = documentParseFactory.getParse(kbDocument.getFileExtension());
            if (parse == null) {
                throw new BusinessException(ResultCode.FAIL, "暂不能解析该文件");
            }
            ParsedText parsedText = parse.parse(kbDocument.getId(), kbDocument.getStoragePath());
            kbDocument.setParsedStoragePath(parsedText.relativePath());
            kbDocument.setParsedPreview(parsedText.preview());
            kbDocument.setParsedCharCount(parsedText.charCount());
            kbDocument.setStatus(KbDocumentStatus.PARSED);
            kbDocument.setErrorMessage(null);
        } catch (Exception e) {
            kbDocument.setStatus(KbDocumentStatus.FAILED);
            kbDocument.setErrorMessage(abbreviate(e.getMessage(), 2000));
        }
        this.updateById(kbDocument);
        return toDto(kbDocument);
    }

    private boolean existsSameFile(UUID knowledgeBaseId, String sha256) {
        long count = this.count(new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KbDocument::getSha256, sha256));
        return count > 0;

    }

    private KbDocumentDto toDto(KbDocument kbDocument) {
        KbDocumentDto documentDto = new KbDocumentDto();
        documentDto.setId(kbDocument.getId());
        documentDto.setKnowledgeBaseId(kbDocument.getKnowledgeBaseId());
        documentDto.setOriginalName(kbDocument.getOriginalName());
        documentDto.setStoredName(kbDocument.getStoredName());
        documentDto.setStoragePath(kbDocument.getStoragePath());
        documentDto.setContentType(kbDocument.getContentType());
        documentDto.setFileExtension(kbDocument.getFileExtension());
        documentDto.setSizeBytes(kbDocument.getSizeBytes());
        documentDto.setSha256(kbDocument.getSha256());
        documentDto.setStatus(kbDocument.getStatus());
        return documentDto;
    }

    private int indexByBatch(KbDocument kbDocument) {
        int lastChunkIndex = -1;
        int total = 0;
        while (true) {
            List<DocumentChunk> chunks = documentChunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                    .eq(DocumentChunk::getDocumentId, kbDocument.getId())
                    .gt(DocumentChunk::getChunkIndex, lastChunkIndex)
                    .orderByAsc(DocumentChunk::getChunkIndex)
                    .last(" limit " + INDEX_BATCH_SIZE)
            );
            if (chunks == null || chunks.isEmpty()) {
                break;
            }
            List<Document> vectorDocuments = new ArrayList<>(chunks.size());
            for (DocumentChunk chunk : chunks) {
                vectorDocuments.add(toVectorDocument(kbDocument, chunk));
            }
            vectorStore.add(vectorDocuments);
            total += chunks.size();
            lastChunkIndex = chunks.get(chunks.size() - 1).getChunkIndex();
        }
        return total;
    }

    private Document toVectorDocument(KbDocument kbDocument, DocumentChunk chunk) {
        //构建meta data 数据
        Map<String, Object> metadata =
                new LinkedHashMap<>();

        metadata.put(
                "knowledgeBaseId",
                chunk.getKnowledgeBaseId().toString()
        );
        metadata.put(
                "documentId",
                chunk.getDocumentId().toString()
        );
        metadata.put(
                "chunkId",
                chunk.getId().toString()
        );
        metadata.put(
                "documentName",
                kbDocument.getOriginalName()
        );
        metadata.put(
                "chunkIndex",
                chunk.getChunkIndex()
        );
        metadata.put(
                "contentHash",
                chunk.getContentHash()
        );

        if (StringUtils.hasText(chunk.getSectionTitle())) {
            metadata.put(
                    "sectionTitle",
                    chunk.getSectionTitle()
            );
        }
        if (chunk.getPageNumber() != null) {
            metadata.put(
                    "pageNumber",
                    chunk.getPageNumber()
            );
        }
        return new Document(chunk.getId().toString(), chunk.getContent(), metadata);
    }

    private void validateDocument(KbDocument document, UUID documentId) {
        if (document == null) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "文档不存在：" + documentId
            );
        }

        boolean canIndex =
                KbDocumentStatus.CHUNKED
                        .equals(document.getStatus())
                        || KbDocumentStatus.INDEXED
                        .equals(document.getStatus())
                        || KbDocumentStatus.INDEX_FAILED
                        .equals(document.getStatus());

        if (!canIndex || document.getChunkCount() == null
                || document.getChunkCount() <= 0) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "文档尚未完成文本切分"
            );
        }
    }

    private void deleteVectorsQuietly(UUID documentId) {
        try {
            vectorStore.delete(
                    "documentId == '" + documentId + "'"
            );
        } catch (Exception ignored) {
            // 后续接入日志记录
        }
    }
}
