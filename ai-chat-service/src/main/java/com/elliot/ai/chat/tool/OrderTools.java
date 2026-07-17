package com.elliot.ai.chat.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elliot.ai.chat.dto.CancelOrderPayload;
import com.elliot.ai.chat.entity.AiActionApproval;
import com.elliot.ai.chat.enums.ActionStatus;
import com.elliot.ai.chat.enums.ActionType;
import com.elliot.ai.chat.enums.OrderInfoStatus;
import com.elliot.ai.chat.exception.BusinessException;
import com.elliot.ai.chat.mapper.AiActionApprovalMapper;
import com.elliot.ai.chat.mapper.OrderInfoMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class OrderTools {

    @Autowired
    private OrderInfoMapper oderInfoMapper;

    @Autowired
    private AiActionApprovalMapper aiActionApprovalMapper;

    @Autowired
    private ObjectMapper objectMapper;


    private static final Map<String, OrderInfo> MOCK_ORDERS = Map.of(
            "OD1001", new OrderInfo("OD1001", "已支付", new BigDecimal("199.00"), "预计明天发货"),
            "OD1002", new OrderInfo("OD1002", "已发货", new BigDecimal("89.90"), "快递单号 SF123456"),
            "OD1003", new OrderInfo("OD1003", "已取消", new BigDecimal("299.00"), "用户主动取消")
    );


    @Tool(
            name = "queryOrder",
            description = "根据订单号查询订单状态、订单金额和物流说明。当用户想查询订单、物流、支付状态时使用这个工具。"
    )
    public String queryOrder(String orderId) {
        OrderInfo order = MOCK_ORDERS.get(orderId);
        if (order == null) {
            return "Order not found";
        }

        return """
                订单号：%s
                状态：%s
                金额：%s 元
                说明：%s
                """.formatted(
                order.orderId(),
                order.status(),
                order.amount(),
                order.remark());
    }


    @Tool(
            name = "queryOrderForDb",
            description = "根据订单号查询订单状态、订单金额和物流说明。当用户想查询在线订单、物流、支付状态时使用这个工具。"

    )
    public String queryOrderForDb(String orderId) {
        com.elliot.ai.chat.entity.OrderInfo orderInfo = oderInfoMapper.selectOne(new LambdaQueryWrapper<com.elliot.ai.chat.entity.OrderInfo>()
                .eq(com.elliot.ai.chat.entity.OrderInfo::getOrderNo, orderId)
                .last("limit 1"));
        if (orderInfo == null) {
            return "Order not found";
        }
        return """
                订单号：%s
                状态：%s
                金额：%s 元
                """.formatted(
                orderInfo.getOrderNo(),
                orderInfo.getStatus(),
                orderInfo.getAmount());
    }


    @Tool(
            name = "createCancelOrder",
            description = "当用户想取消订单时使用该工具。该工具只是创建人工待确定的取消订单申请，不会真正取消订单"
    )
    public String createCancelOrder(
            @ToolParam(description = "订单号如983") String orderNo,
            @ToolParam(description = "取消订单的原因") String cancelReason
    ) {
        com.elliot.ai.chat.entity.OrderInfo orderInfo = oderInfoMapper.selectOne(new LambdaQueryWrapper<com.elliot.ai.chat.entity.OrderInfo>()
                .eq(com.elliot.ai.chat.entity.OrderInfo::getOrderNo, orderNo)
                .last("limit 1"));
        if (orderInfo == null) {
            return """
                    cannot found order no is %s
                    """.formatted(orderNo);
        }
        if (OrderInfoStatus.CANCEL.name().equals(orderInfo.getStatus())) {
            return """
                            order is cancelled %s
                    """.formatted(orderNo);
        }
        //创建取消订单
        CancelOrderPayload payload = new CancelOrderPayload(orderInfo.getId(), cancelReason);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException("PAYLOAD_BUILD_ERROR", "创建取消订单参数失败");
        }
        AiActionApproval aiActionApproval = new AiActionApproval();
        aiActionApproval.setActionType(ActionType.CANCEL_ORDER);
        aiActionApproval.setPayload(payloadJson);
        aiActionApproval.setStatus(ActionStatus.PENDING);
        aiActionApproval.setCreatedAt(LocalDateTime.now());
        aiActionApproval.setUpdatedAt(LocalDateTime.now());
        aiActionApprovalMapper.insert(aiActionApproval);
        return """
                已创建取消订单待确认操作。
                actionId：%s
                订单号：%s
                原因：%s
                
                注意：订单尚未真正取消，需要用户确认后才会执行。
                请提示用户调用确认接口完成操作。
                """.formatted(aiActionApproval.getId(), orderNo, cancelReason);
    }


    public record OrderInfo(
            String orderId,
            String status,
            BigDecimal amount,
            String remark
    ) {
    }


}
