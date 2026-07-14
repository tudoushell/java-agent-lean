package com.elliot.ai.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.elliot.ai.rag.enums.KbDocumentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 对应 kb_document 表。 */
@Getter
@Setter
@TableName("kb_document")
public class KbDocument {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    private UUID knowledgeBaseId;

    private String originalName;

    private String storedName;

    private String storagePath;

    private String contentType;

    private String fileExtension;

    private Long sizeBytes;

    private String sha256;

    private KbDocumentStatus status;

    private String parsedStoragePath;

    private String parsedPreview;

    private Long parsedCharCount;

    private String errorMessage;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
