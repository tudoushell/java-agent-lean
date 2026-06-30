package com.elliot.ai.chat.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.elliot.ai.chat.entity.AiActionApproval;
import org.springframework.stereotype.Service;

@Service
public interface ActionApprovalService extends IService<AiActionApproval> {

    void rejectApproval(String approvalId);

    void confirmApproval(String approvalId);
}
