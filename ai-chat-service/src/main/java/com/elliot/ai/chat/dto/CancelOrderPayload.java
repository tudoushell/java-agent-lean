package com.elliot.ai.chat.dto;

import lombok.Getter;

public record CancelOrderPayload(Long orderId, String reason) {

}
