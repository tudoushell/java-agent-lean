package com.elliot.ai.rag.service.impl;

import com.elliot.ai.rag.mapper.DocumentProcessTaskMapper;
import com.elliot.ai.rag.service.DocumentTaskClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DocumentTaskClaimServiceImpl implements DocumentTaskClaimService {

    private final DocumentProcessTaskMapper documentProcessTaskMapper;
    private final TransactionTemplate transactionTemplate;
    private final DocumentProcessTaskMapper taskMapper;

    @Override
    public List<UUID> claim(int limit, String workId) {

        return List.of();
    }
}
