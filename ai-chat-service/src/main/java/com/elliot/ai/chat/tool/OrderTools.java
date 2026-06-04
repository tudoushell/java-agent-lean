package com.elliot.ai.chat.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.elliot.ai.chat.mapper.OrderInfoMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class OrderTools {

    @Autowired
    private OrderInfoMapper oderInfoMapper;

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


    public record OrderInfo(
            String orderId,
            String status,
            BigDecimal amount,
            String remark
    ) {
    }

}
