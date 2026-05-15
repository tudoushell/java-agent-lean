package com.elliot.ai.chat.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"summary", "keyPoints", "suggestions"})
public record SummaryResponse(String summary,
                              List<String> keyPoints,
                              List<String> suggestions) {

}