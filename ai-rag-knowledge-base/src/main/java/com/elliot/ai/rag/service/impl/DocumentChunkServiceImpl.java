package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.ChunkProperties;
import com.elliot.ai.rag.config.StorageProperties;
import com.elliot.ai.rag.entity.DocumentChunk;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import com.elliot.ai.rag.mapper.DocumentChunkMapper;
import com.elliot.ai.rag.mapper.KbDocumentMapper;
import com.elliot.ai.rag.service.DocumentChunkService;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * 文档文本片段业务服务实现。
 */
@Service
public class DocumentChunkServiceImpl
        extends ServiceImpl<DocumentChunkMapper, DocumentChunk>
        implements DocumentChunkService {

    private static final int FILE_READ_BUFFER_SIZE = 4096;
    private final KbDocumentMapper kbDocumentMapper;
    private final ChunkProperties chunkProperties;
    private final StorageProperties storageProperties;
    private final Path parsedRootPath;
    private final TokenTextSplitter tokenTextSplitter;

    public DocumentChunkServiceImpl(KbDocumentMapper kbDocumentMapper,
                                    ChunkProperties chunkProperties,
                                    StorageProperties storageProperties,
                                    TokenTextSplitter tokenTextSplitter) {
        this.kbDocumentMapper = kbDocumentMapper;
        this.chunkProperties = chunkProperties;
        this.storageProperties = storageProperties;
        this.parsedRootPath = Paths.get(storageProperties.getParsedDirectory())
                .toAbsolutePath().normalize();
        this.tokenTextSplitter = tokenTextSplitter;
    }

    @Override
    public int chunk(UUID documentId) {
        KbDocument kbDocument = kbDocumentMapper.selectById(documentId);
        if (kbDocument == null) {
            throw new BusinessException(ResultCode.FAIL, "文档不存在");
        }
        if (!KbDocumentStatus.PARSED.equals(kbDocument.getStatus())
                && !KbDocumentStatus.CHUNKED.equals(kbDocument.getStatus())) {
            throw new BusinessException(ResultCode.FAIL, "文档尚未解析完成");
        }
        // 重新切分时，先删除旧 Chunk。
        this.baseMapper.delete(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId));
        int chunkCount = streamAndSaveChunks(kbDocument);
        if (chunkCount == 0) {
            throw new BusinessException(ResultCode.FAIL, "文档切分结果为空");
        }
        kbDocument.setStatus(KbDocumentStatus.CHUNKED);
        kbDocument.setChunkCount(chunkCount);
        kbDocument.setChunkStrategy("TOKEN");
        kbDocument.setChunkSize(chunkProperties.getChunkSize());
        kbDocument.setChunkOverlap(chunkProperties.getOverlap());
        kbDocument.setErrorMessage(null);
        kbDocumentMapper.updateById(kbDocument);
        return chunkCount;
    }

    private int streamAndSaveChunks(KbDocument kbDocument) {
        List<DocumentChunk> batch = new ArrayList<>(chunkProperties.getDatabaseBatchSize());
        int nextChunkIndex = 0;
        int blockLimit = chunkProperties.getReadBufferChars();
        Path parsedFile = parsedRootPath.resolve(kbDocument.getParsedStoragePath()).normalize();
        try (BufferedReader reader = Files.newBufferedReader(parsedFile)) {
            char[] readBuffer = new char[FILE_READ_BUFFER_SIZE];
            StringBuilder pending = new StringBuilder(blockLimit + FILE_READ_BUFFER_SIZE);
            int readLength;
            while ((readLength = reader.read(readBuffer)) != -1) {
                pending.append(readBuffer, 0, readLength);
                if (pending.length() >= blockLimit) {
                    int cutPoint = findCutPoint(pending, blockLimit);
                    String block = pending.substring(0, cutPoint);
                    pending.delete(0, cutPoint);
                    nextChunkIndex = splitBlockAndCollect(kbDocument, block, nextChunkIndex, batch);
                }
            }
            if (!pending.isEmpty()) {
                splitBlockAndCollect(kbDocument, pending.toString(), nextChunkIndex, batch);
            }
            flushBatch(batch);
            return nextChunkIndex;
        } catch (IOException e) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "读取解析文本失败"
            );
        }
    }

    private int splitBlockAndCollect(KbDocument kbDocument, String block, int nextChunkIndex, List<DocumentChunk> batch) {
        if (!StringUtils.hasText(block)) {
            return nextChunkIndex;
        }
        Document document = new Document(block, Map.of(
                "knowledgeBaseId",
                kbDocument
                        .getKnowledgeBaseId()
                        .toString(),
                "documentId",
                kbDocument
                        .getId()
                        .toString(),
                "documentName",
                kbDocument
                        .getOriginalName()));
        List<Document> splitDocuments = tokenTextSplitter.apply(List.of(document));
        for (Document splitDocument : splitDocuments) {
            String content = splitDocument.getText();
            if (!StringUtils.hasText(content)) {
                continue;
            }
            String normalizedContent = content.trim();
            DocumentChunk documentChunk = buildDocumentChunk(kbDocument, nextChunkIndex, normalizedContent);
            batch.add(documentChunk);
            nextChunkIndex++;
            if (batch.size() >= chunkProperties.getDatabaseBatchSize()) {
                flushBatch(batch);
            }
        }
        return nextChunkIndex;
    }

    private DocumentChunk buildDocumentChunk(KbDocument kbDocument, int chunkIndex, String normalizedContent) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setKnowledgeBaseId(
                kbDocument.getKnowledgeBaseId()
        );
        chunk.setDocumentId(kbDocument.getId());
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(normalizedContent);
        chunk.setCharCount(normalizedContent.length());

        // TokenTextSplitter 当前没有直接返回
        // 每个 Chunk 的精确 Token 数，先保存 null。
        chunk.setTokenCount(null);

        chunk.setContentHash(sha256(normalizedContent));
        chunk.setSectionTitle(null);
        chunk.setPageNumber(null);
        return chunk;
    }

    private String sha256(String normalizedContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalizedContent.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FAIL, "计算Chunk hash Failed" + e.getMessage());
        }
    }

    /**
     * 在待处理文本中寻找适合切出的字符下标，优先保持段落和换行完整。
     *
     * @param pending        当前已读取但尚未切分的文本
     * @param preferredLimit 期望的最大字符数
     * @return 前一个文本块的结束下标，可用于 {@code pending.substring(0, cutPoint)}
     */
    private int findCutPoint(StringBuilder pending, int preferredLimit) {
        // 当前可选择的最大切点不能超过已有文本长度。
        int limit = Math.min(preferredLimit, pending.length());
        // 只接受后半段的自然边界，避免切出过短的文本块。
        int minimumBoundary = limit / 2;

        // 优先在空行（段落边界）之后切分，并将空行归入前一个文本块。
        int paragraphIndex = pending.lastIndexOf("\n\n", limit);
        if (paragraphIndex >= minimumBoundary) {
            return paragraphIndex + 2;
        }

        // 没有合适的段落边界时，退化为在最后一个换行符之后切分。
        int lineIndex = pending.lastIndexOf("\n", limit);
        if (lineIndex >= minimumBoundary) {
            return lineIndex + 1;
        }

        // 没有自然边界时按期望长度硬切。
        int cutPoint = limit;
        if (cutPoint < pending.length() && cutPoint > 0
                && Character.isHighSurrogate(pending.charAt(cutPoint - 1))
                && Character.isLowSurrogate(pending.charAt(cutPoint))) {
            // 避免切点落在 Emoji 等 UTF-16 代理对字符的中间。
            cutPoint--;
        }
        return cutPoint;
    }

    private void flushBatch(List<DocumentChunk> batch) {
        if (batch.isEmpty()) {
            return;
        }
        saveBatch(batch);
        batch.clear();
    }

}
