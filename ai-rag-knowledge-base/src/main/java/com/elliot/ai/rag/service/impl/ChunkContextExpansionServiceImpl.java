package com.elliot.ai.rag.service.impl;

import com.elliot.ai.rag.config.RagProperties;
import com.elliot.ai.rag.dto.ExpandedSource;
import com.elliot.ai.rag.dto.RagSourceChunkDto;
import com.elliot.ai.rag.dto.RetrievalHitDto;
import com.elliot.ai.rag.entity.DocumentChunk;
import com.elliot.ai.rag.service.ChunkContextExpansionService;
import com.elliot.ai.rag.service.DocumentChunkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ChunkContextExpansionServiceImpl implements ChunkContextExpansionService {

    private final DocumentChunkService documentChunkService;
    private final RagProperties ragProperties;

    @Override
    public ExpandedSource expand(RetrievalHitDto hit) {
        int radius = ragProperties.getAdjacentChunkRadius();
        Integer matchedIndex = hit.chunkIndex();
        int startIndex = Math.max(0, matchedIndex - radius);
        int endIndex = matchedIndex + radius;
        List<DocumentChunk> chunks = documentChunkService.selectNeighborChunks(hit.documentId(), startIndex, endIndex);
        if (chunks.isEmpty()) {
            return fallback(hit);
        }
        int totalChars = 0;
        List<RagSourceChunkDto> sourceChunks = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            boolean matched = isMatched(hit, chunk);

            if (matched && totalChars + chunk.getContent().length() > ragProperties.getMaxSourceChars()) {
                continue;
            }
            sourceChunks.add(new RagSourceChunkDto(
                    chunk.getId(),
                    chunk.getChunkIndex(),
                    chunk.getSectionTitle(),
                    chunk.getPageNumber(),
                    chunk.getContent(),
                    matched
            ));
            totalChars += chunk.getContent().length();
        }
        boolean containsMatchedChunk = sourceChunks.stream().anyMatch(RagSourceChunkDto::matched);
        if (!containsMatchedChunk) {
            return fallback(hit);
        }
        Integer actualStart = sourceChunks.get(0).chunkIndex();
        Integer actualEnd = sourceChunks.get(sourceChunks.size() - 1).chunkIndex();
        return new ExpandedSource(
                hit.rank(),
                hit.score(),
                hit.chunkId(),
                hit.chunkIndex(),
                hit.documentId(),
                hit.documentName(),
                actualStart,
                actualEnd,
                List.copyOf(sourceChunks));
    }

    private boolean isMatched(RetrievalHitDto hit, DocumentChunk chunk) {
        if (hit.chunkId() != null) {
            return hit.chunkId().equals(chunk.getId());
        }
        return hit.chunkIndex() != null && hit.chunkIndex().equals(chunk.getChunkIndex());
    }

    private ExpandedSource fallback(RetrievalHitDto hit) {
        RagSourceChunkDto chunk = new RagSourceChunkDto(
                hit.chunkId(),
                hit.chunkIndex(),
                hit.sectionTitle(),
                hit.pageNumber(),
                hit.content(),
                true
        );
        return new ExpandedSource(
                hit.rank(),
                hit.score(),
                hit.chunkId(),
                hit.chunkIndex(),
                hit.documentId(),
                hit.documentName(),
                hit.chunkIndex(),
                hit.chunkIndex(),
                List.of(chunk)
        );
    }
}
