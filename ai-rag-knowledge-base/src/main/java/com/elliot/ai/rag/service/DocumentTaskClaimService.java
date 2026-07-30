package com.elliot.ai.rag.service;

import java.util.List;
import java.util.UUID;

public interface DocumentTaskClaimService {
    List<UUID> claim(int limit, String workId);
}
