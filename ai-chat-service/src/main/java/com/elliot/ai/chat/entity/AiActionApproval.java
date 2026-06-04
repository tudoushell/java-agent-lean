package com.elliot.ai.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.elliot.ai.chat.enums.ActionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@TableName("ai_action_approval")
public class AiActionApproval {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String actionType;
    private ActionStatus status;
    private String description;
    private String payload;
    private String resultMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
