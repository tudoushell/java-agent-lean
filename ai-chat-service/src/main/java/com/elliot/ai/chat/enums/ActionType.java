package com.elliot.ai.chat.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ActionType {

    CANCEL_ORDER("取消订单");

    @EnumValue
    private final String code;
    private final String desc;

    ActionType(String desc) {
        this.code = name();
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
