package com.elliot.ai.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elliot.ai.rag.entity.DocumentChunk;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

/** 文档文本片段数据访问接口。 */
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 按 Chunk 顺序分页查询指定文档的文本片段。
     */
    @Select("""
            select *
            from document_chunk
            where document_id = #{documentId}
            order by chunk_index asc
            limit #{size} offset #{offset}
            """)
    List<DocumentChunk> selectPageByDocumentId(
            @Param("documentId") UUID documentId,
            @Param("offset") long offset,
            @Param("size") int size
    );

    /**
     * 统计指定文档的 Chunk 总数。
     */
    @Select("""
            select count(*)
            from document_chunk
            where document_id = #{documentId}
            """)
    long countByDocumentId(@Param("documentId") UUID documentId);
}
