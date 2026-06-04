package com.elliot.ai.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elliot.ai.chat.dto.CancelOrderPayload;
import com.elliot.ai.chat.entity.AiActionApproval;
import com.elliot.ai.chat.entity.OrderInfo;
import com.elliot.ai.chat.enums.ActionStatus;
import com.elliot.ai.chat.enums.OrderInfoStatus;
import com.elliot.ai.chat.exception.BusinessException;
import com.elliot.ai.chat.mapper.AiActionApprovalMapper;
import com.elliot.ai.chat.mapper.OrderInfoMapper;
import com.elliot.ai.chat.service.ActionApprovalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ActionApprovalServiceImpl extends ServiceImpl<AiActionApprovalMapper, AiActionApproval>
        implements ActionApprovalService {

    private final OrderInfoMapper orderInfoMapper;

    @Override
    public void rejectApproval(String approvalId) {
        AiActionApproval aiActionApproval = this.getById(approvalId);
        if (aiActionApproval == null) {
            throw new BusinessException("""
                    Approval with ID '%s' not found""".formatted(approvalId));
        }
        if (ActionStatus.PENDING.equals(aiActionApproval.getStatus())) {
            throw new BusinessException("this action is pending");
        }
        aiActionApproval.setStatus(ActionStatus.REJECTED);
        aiActionApproval.setDescription("rejected by user");
        this.updateById(aiActionApproval);
    }

    private void confirmCancelOrder(String payload) {
        CancelOrderPayload cancelOrderPayload = this.parsePayload(payload);
        if (cancelOrderPayload == null) {
            throw new BusinessException("cannot parse payload");
        }
        Long orderId = cancelOrderPayload.orderId();
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (OrderInfoStatus.CANCEL.name().equals(orderInfo.getStatus())) {
            throw new BusinessException("order is already cancelled");
        }
        orderInfo.setStatus(OrderInfoStatus.CANCEL.name());
        orderInfo.setRemark(cancelOrderPayload.reason());
        int updated = orderInfoMapper.updateById(orderInfo);
        if (updated != 1) {
            throw new BusinessException("update order status failed");
        }
    }

    @Override
    public void confirmApproval(String approvalId) {
        AiActionApproval aiActionApproval = this.getById(approvalId);
        if (aiActionApproval == null) {
            throw new BusinessException("""
                    Approval with ID '%s' not found""".formatted(approvalId));
        }
        if (ActionStatus.PENDING.equals(aiActionApproval.getStatus())) {
            throw new BusinessException("this action is pending");
        }
        if ("CANCEL_ORDER".equals(aiActionApproval.getActionType())) {
            confirmCancelOrder(aiActionApproval.getPayload());
        }
        throw new BusinessException("cannot support this action type");

    }


    private CancelOrderPayload parsePayload(String payload) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(payload, CancelOrderPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
