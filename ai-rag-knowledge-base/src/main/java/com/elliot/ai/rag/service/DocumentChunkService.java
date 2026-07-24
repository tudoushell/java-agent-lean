package com.elliot.ai.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elliot.ai.rag.entity.DocumentChunk;

import java.util.List;
import java.util.UUID;

/** 文档文本片段业务服务。 */
public interface DocumentChunkService extends IService<DocumentChunk> {

    /**
     * 查询相邻的chunk
     *
     * @param documentId
     * @param startIndex
     * @param endIndex
     * @return
     */
    List<DocumentChunk> selectNeighborChunks(UUID documentId, int startIndex, int endIndex);

    /**
     * 文档内容进行chunk
     *
     * @param documentId
     * @return
     */
    int chunk(UUID documentId);
}
