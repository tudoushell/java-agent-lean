package com.elliot.ai.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elliot.ai.rag.entity.DocumentProcessTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文档处理任务数据访问 Mapper。
 */
public interface DocumentProcessTaskMapper extends BaseMapper<DocumentProcessTask> {


    /**
     * 超时但还有重试次数的任务进入 RETRY_WAIT
     *
     * @param nextRetryAt
     * @param now
     */
    @Update("""
              update document_process_task
              set
                 status = 'RETRY_WAIT',
                 retry_count = retry_count + 1,
                 next_retry_at = #{nextRetryAt},
                 worker_id = NULL,
                 locked_at = NULL,
                 heartbeat_at = NULL,
                 error_code = 'TASK_EXECUTION_TIMEOUT',
                 error_message = '任务执行超时或服务异常退出',
                 updated_at = #{now}
              where status = 'RUNNING'
                    and heartbeat_at < #{cutoff}
                    and retry_count < max_retries
            """)
    void recoverRetryableStaleTasks(@Param("cutoff") OffsetDateTime cutoff, @Param("nextRetryAt") OffsetDateTime nextRetryAt, @Param("now") OffsetDateTime now);


    /**
     * 没有超时的直接失败
     *
     * @param cutoff
     * @param now
     */
    @Update("""
               update document_process_task
               set 
                  status = 'FAILED',
                  worker_id = NULL,
                  locked_at = NULL,
                  heartbeat_at = NULL,
                  error_code = 'TASK_RETRY_EXHAUSTED',
                  error_message = '任务超时且已达到最大重试次数',
                  finished_at = #{now},
                  updated_at = #{now}
               where status = 'RUNNING'
               and heartbeat_at < #{cutoff}
               and retry_count >= max_retries 
            """)
    void failExhaustedStaleTask(@Param("cutoff") OffsetDateTime cutoff, @Param("now") OffsetDateTime now);

    @Select("""
            select id
            from document_process_task
            where status = 'PENDING'
            or (
                status = 'RETRY_WAIT'
                and next_retry_at <= CURRENT_TIMESTAMP
               )
            order by created_at
            for update skip locked
            limit #{limit}
           """)
    List<UUID> selectClaimableTaskIds(@Param("limit") int limit);
}
