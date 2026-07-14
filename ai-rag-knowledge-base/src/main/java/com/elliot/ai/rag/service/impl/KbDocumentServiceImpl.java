package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elliot.ai.rag.entity.KbDocument;
import com.elliot.ai.rag.mapper.KbDocumentMapper;
import com.elliot.ai.rag.service.KbDocumentService;
import org.springframework.stereotype.Service;

@Service
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument>
        implements KbDocumentService {
}
