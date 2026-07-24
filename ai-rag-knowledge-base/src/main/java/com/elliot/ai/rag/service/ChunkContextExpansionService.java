package com.elliot.ai.rag.service;

import com.elliot.ai.rag.dto.ExpandedSource;
import com.elliot.ai.rag.dto.RetrievalHitDto;

public interface ChunkContextExpansionService {

    ExpandedSource expand(RetrievalHitDto retrievalHitDto);
}
