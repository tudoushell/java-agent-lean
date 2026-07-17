package com.elliot.ai.chat.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ActionStatus {

    PENDING("待确认"),
    EXECUTED("已执行"),
    REJECTED("已拒绝"),
    FAILED("执行失败");

    @EnumValue
    private final String code;
    private final String desc;

    ActionStatus(String desc) {
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
