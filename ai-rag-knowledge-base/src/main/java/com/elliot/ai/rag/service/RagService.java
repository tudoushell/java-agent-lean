package com.elliot.ai.rag.service;

import com.elliot.ai.rag.dto.RagChatDto;
import com.elliot.ai.rag.dto.RagChatResultDto;

public interface RagService {

    RagChatResultDto ragChat(RagChatDto ragChatDto);
}
