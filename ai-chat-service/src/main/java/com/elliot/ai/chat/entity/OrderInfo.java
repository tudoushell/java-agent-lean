package com.elliot.ai.chat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@TableName("order_info")
public class OrderInfo {
    @TableId
    private Long id;
    private String userName;
    private String orderNo;
    private String amount;
    private String status;
    private String remark;
    private Date createTime;
}
