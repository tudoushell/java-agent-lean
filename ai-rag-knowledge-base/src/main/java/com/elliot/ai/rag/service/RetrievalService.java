package com.elliot.ai.rag.service;

import com.elliot.ai.rag.dto.RetrievalSearchDto;
import com.elliot.ai.rag.dto.RetrievalSearchResultDto;

public interface RetrievalService {

    RetrievalSearchResultDto search(RetrievalSearchDto searchDto);
}
