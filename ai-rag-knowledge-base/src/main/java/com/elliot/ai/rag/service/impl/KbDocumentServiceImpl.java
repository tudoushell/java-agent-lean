package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.dto.KbDocumentDto;
import com.elliot.ai.rag.dto.ParsedText;
import com.elliot.ai.rag.dto.StoredFile;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.entity.KnowledgeBase;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import com.elliot.ai.rag.enums.KnowledgeBaseStatus;
import com.elliot.ai.rag.factory.DocumentParseFactory;
import com.elliot.ai.rag.mapper.KbDocumentMapper;
import com.elliot.ai.rag.mapper.KnowledgeBaseMapper;
import com.elliot.ai.rag.parse.AbstractDocumentParse;
import com.elliot.ai.rag.service.KbDocumentService;
import com.elliot.ai.rag.service.LocalFilesStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.abbreviate;

@RequiredArgsConstructor
@Service
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument>
        implements KbDocumentService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final LocalFilesStorageService localFilesStorageService;
    private final DocumentParseFactory documentParseFactory;


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
}
