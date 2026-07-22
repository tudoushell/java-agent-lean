const state = {
    libraries: [],
    activeLibrary: null,
    documents: JSON.parse(localStorage.getItem('atlas-documents') || '{}')
};

const elements = {
    libraryList: document.querySelector('#library-list'),
    activeName: document.querySelector('#active-name'),
    activeDescription: document.querySelector('#active-description'),
    activeBadge: document.querySelector('#active-badge'),
    apiStatus: document.querySelector('#api-status'),
    createForm: document.querySelector('#create-form'),
    createToggle: document.querySelector('#create-toggle'),
    fileInput: document.querySelector('#document-file'),
    fileName: document.querySelector('#file-name'),
    documentId: document.querySelector('#document-id'),
    documentSummary: document.querySelector('#document-summary'),
    chunkButton: document.querySelector('#chunk-document'),
    indexButton: document.querySelector('#index-document'),
    retrievalOutput: document.querySelector('#retrieval-output'),
    chatOutput: document.querySelector('#chat-output'),
    toast: document.querySelector('#toast')
};

let toastTimer;

async function api(path, options = {}) {
    const response = await fetch(path, options);
    const body = await response.json().catch(() => null);
    if (!response.ok || !body || body.code !== 200) {
        throw new Error(body?.message || `请求失败（HTTP ${response.status}）`);
    }
    return body.data;
}

function notify(message, type = 'success') {
    clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.className = `toast visible${type === 'error' ? ' error' : ''}`;
    toastTimer = setTimeout(() => elements.toast.className = 'toast', 3200);
}

async function withBusy(button, task) {
    button.classList.add('busy');
    button.disabled = true;
    try {
        return await task();
    } finally {
        button.classList.remove('busy');
        button.disabled = false;
        updateDocumentActions();
    }
}

function requireLibrary() {
    if (!state.activeLibrary) {
        notify('请先从左侧选择知识库', 'error');
        return false;
    }
    return true;
}

function formatDate(value) {
    if (!value) return '时间未知';
    return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric' }).format(new Date(value));
}

async function loadLibraries(preferredId) {
    elements.libraryList.innerHTML = '<div class="library-empty">正在读取知识库…</div>';
    try {
        state.libraries = await api('/api/knowledge-bases');
        elements.apiStatus.textContent = '已连接';
        elements.apiStatus.classList.remove('error');
        renderLibraries();
        const target = preferredId || state.activeLibrary?.id || localStorage.getItem('atlas-active-library');
        const library = state.libraries.find(item => item.id === target) || state.libraries[0];
        if (library) await selectLibrary(library.id);
        else clearActiveLibrary();
    } catch (error) {
        elements.apiStatus.textContent = '连接失败';
        elements.apiStatus.classList.add('error');
        elements.libraryList.innerHTML = '<div class="library-empty">无法读取知识库，请确认服务已启动。</div>';
        notify(error.message, 'error');
    }
}

function renderLibraries() {
    elements.libraryList.replaceChildren();
    if (!state.libraries.length) {
        const empty = document.createElement('div');
        empty.className = 'library-empty';
        empty.textContent = '还没有知识库，先创建一个。';
        elements.libraryList.append(empty);
        return;
    }
    state.libraries.forEach(library => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = `library-item${state.activeLibrary?.id === library.id ? ' active' : ''}`;
        const name = document.createElement('strong');
        name.textContent = library.name;
        const meta = document.createElement('small');
        meta.textContent = `${library.status || 'UNKNOWN'} · ${formatDate(library.updatedAt || library.createdAt)}`;
        button.append(name, meta);
        button.addEventListener('click', () => selectLibrary(library.id));
        elements.libraryList.append(button);
    });
}

async function selectLibrary(id) {
    try {
        state.activeLibrary = await api(`/api/knowledge-bases/${encodeURIComponent(id)}`);
        localStorage.setItem('atlas-active-library', id);
        renderLibraries();
        renderActiveLibrary();
        restoreDocument();
    } catch (error) {
        notify(error.message, 'error');
    }
}

function clearActiveLibrary() {
    state.activeLibrary = null;
    elements.activeName.textContent = '选择一个知识库';
    elements.activeDescription.textContent = '从左侧选择知识库，开始整理与检索资料。';
    elements.activeBadge.textContent = '未选择';
    elements.activeBadge.className = 'active-badge';
    elements.documentId.value = '';
    renderDocument(null);
}

function renderActiveLibrary() {
    const library = state.activeLibrary;
    elements.activeName.textContent = library.name;
    elements.activeDescription.textContent = library.description || `知识库 ID：${library.id}`;
    elements.activeBadge.textContent = library.status || 'UNKNOWN';
    elements.activeBadge.className = `active-badge${library.status === 'ENABLED' ? ' enabled' : ''}`;
}

function persistDocument(document) {
    if (!state.activeLibrary || !document) return;
    state.documents[state.activeLibrary.id] = document;
    localStorage.setItem('atlas-documents', JSON.stringify(state.documents));
}

function restoreDocument() {
    const document = state.documents[state.activeLibrary.id] || null;
    elements.documentId.value = document?.id || '';
    renderDocument(document);
}

function currentDocument() {
    const id = elements.documentId.value.trim();
    if (!id || !state.activeLibrary) return null;
    const saved = state.documents[state.activeLibrary.id];
    return saved?.id === id ? saved : { id, status: 'UNKNOWN', originalName: '已有文档' };
}

function renderDocument(documentInfo) {
    elements.documentSummary.replaceChildren();
    if (!documentInfo) {
        elements.documentSummary.className = 'document-summary empty';
        elements.documentSummary.textContent = '尚未选择文档';
        setPipeline('upload');
        updateDocumentActions();
        return;
    }
    elements.documentSummary.className = 'document-summary';
    const title = document.createElement('strong');
    title.textContent = documentInfo.originalName || '已有文档';
    const idLine = document.createElement('span');
    idLine.textContent = `ID：${documentInfo.id}`;
    const stateLine = document.createElement('span');
    const extras = [];
    if (documentInfo.chunkCount != null) extras.push(`${documentInfo.chunkCount} 个 Chunk`);
    if (documentInfo.vectorCount != null) extras.push(`${documentInfo.vectorCount} 条向量`);
    stateLine.textContent = `状态：${documentInfo.status || 'UNKNOWN'}${extras.length ? ` · ${extras.join(' · ')}` : ''}`;
    elements.documentSummary.append(title, idLine, document.createElement('br'), stateLine);
    setPipeline(documentInfo.status);
    updateDocumentActions();
}

function setPipeline(status) {
    const normalized = String(status || '').toUpperCase();
    const level = ['INDEXED', 'INDEXING', 'INDEX_FAILED'].includes(normalized) ? 3
        : normalized === 'CHUNKED' ? 2
        : ['PARSED', 'UPLOADED', 'UNKNOWN', 'UPLOAD'].includes(normalized) ? 1 : 1;
    document.querySelectorAll('.pipeline-step').forEach((step, index) => {
        step.classList.toggle('done', index + 1 < level);
        step.classList.toggle('current', index + 1 === level);
    });
    document.querySelectorAll('.pipeline-link').forEach((link, index) => link.classList.toggle('done', index + 1 < level));
}

function updateDocumentActions() {
    const enabled = Boolean(state.activeLibrary && elements.documentId.value.trim());
    elements.chunkButton.disabled = !enabled;
    elements.indexButton.disabled = !enabled;
}

function switchView(name) {
    document.querySelectorAll('.view-tab').forEach(tab => tab.classList.toggle('active', tab.dataset.view === name));
    document.querySelectorAll('.view').forEach(view => view.classList.toggle('active', view.id === `view-${name}`));
}

function hitCard(hit, label) {
    const card = document.createElement('article');
    card.className = 'hit';
    const top = document.createElement('div');
    top.className = 'hit-top';
    const title = document.createElement('div');
    title.className = 'hit-title';
    title.textContent = `${label || `#${hit.rank}`} · ${hit.documentName || '未命名文档'}`;
    const score = document.createElement('span');
    score.className = 'hit-score';
    score.textContent = hit.score == null ? 'NO SCORE' : Number(hit.score).toFixed(4);
    top.append(title, score);
    const meta = document.createElement('div');
    meta.className = 'hit-meta';
    const details = [];
    if (hit.sectionTitle) details.push(hit.sectionTitle);
    if (hit.pageNumber != null) details.push(`第 ${hit.pageNumber} 页`);
    if (hit.chunkIndex != null) details.push(`Chunk ${hit.chunkIndex}`);
    meta.textContent = details.join(' · ') || `文档 ID：${hit.documentId || '-'}`;
    const content = document.createElement('p');
    content.className = 'hit-content';
    content.textContent = hit.content || '';
    card.append(top, meta, content);
    return card;
}

function renderRetrieval(data) {
    elements.retrievalOutput.className = 'output-panel';
    elements.retrievalOutput.replaceChildren();
    const head = document.createElement('div');
    head.className = 'result-head';
    const title = document.createElement('h3');
    title.textContent = '检索结果';
    const meta = document.createElement('span');
    meta.textContent = `${data.resultCount} 条命中 · TopK ${data.topK} · 阈值 ${data.similarityThreshold}`;
    head.append(title, meta);
    const list = document.createElement('div');
    list.className = 'result-list';
    if (!data.hits?.length) {
        const notice = document.createElement('div');
        notice.className = 'notice';
        notice.textContent = '没有达到相似度阈值的片段。可以降低阈值，或检查文档是否已完成索引。';
        list.append(notice);
    } else {
        data.hits.forEach(hit => list.append(hitCard(hit)));
    }
    elements.retrievalOutput.append(head, list);
}

function appendInlineMarkdown(parent, source) {
    const pattern = /(`[^`]+`|\*\*[^*]+\*\*|~~[^~]+~~|\*[^*]+\*|\[[^\]]+\]\([^\s)]+\))/g;
    let cursor = 0;
    for (const match of source.matchAll(pattern)) {
        if (match.index > cursor) {
            parent.append(document.createTextNode(source.slice(cursor, match.index)));
        }
        const token = match[0];
        if (token.startsWith('`')) {
            const code = document.createElement('code');
            code.textContent = token.slice(1, -1);
            parent.append(code);
        } else if (token.startsWith('**')) {
            const strong = document.createElement('strong');
            strong.textContent = token.slice(2, -2);
            parent.append(strong);
        } else if (token.startsWith('~~')) {
            const deleted = document.createElement('del');
            deleted.textContent = token.slice(2, -2);
            parent.append(deleted);
        } else if (token.startsWith('*')) {
            const emphasis = document.createElement('em');
            emphasis.textContent = token.slice(1, -1);
            parent.append(emphasis);
        } else {
            const linkMatch = token.match(/^\[([^\]]+)]\(([^\s)]+)\)$/);
            const url = linkMatch?.[2] || '';
            if (/^(https?:|mailto:)/i.test(url)) {
                const link = document.createElement('a');
                link.href = url;
                link.target = '_blank';
                link.rel = 'noopener noreferrer';
                link.textContent = linkMatch[1];
                parent.append(link);
            } else {
                parent.append(document.createTextNode(token));
            }
        }
        cursor = match.index + token.length;
    }
    if (cursor < source.length) {
        parent.append(document.createTextNode(source.slice(cursor)));
    }
}

function isTableDivider(line) {
    const cells = line.trim().replace(/^\||\|$/g, '').split('|');
    return cells.length > 1 && cells.every(cell => /^\s*:?-{3,}:?\s*$/.test(cell));
}

function tableCells(line) {
    return line.trim().replace(/^\||\|$/g, '').split('|').map(cell => cell.trim());
}

function renderMarkdown(markdown) {
    const fragment = document.createDocumentFragment();
    const lines = String(markdown || '').replace(/\r\n?/g, '\n').split('\n');
    let index = 0;

    while (index < lines.length) {
        const line = lines[index];
        if (!line.trim()) {
            index++;
            continue;
        }

        const fence = line.match(/^\s*```\s*([\w.+-]*)\s*$/);
        if (fence) {
            const block = document.createElement('div');
            block.className = 'markdown-code-block';
            if (fence[1]) {
                const language = document.createElement('div');
                language.className = 'markdown-code-language';
                language.textContent = fence[1];
                block.append(language);
            }
            const pre = document.createElement('pre');
            const code = document.createElement('code');
            const content = [];
            index++;
            while (index < lines.length && !/^\s*```\s*$/.test(lines[index])) {
                content.push(lines[index++]);
            }
            if (index < lines.length) index++;
            code.textContent = content.join('\n');
            pre.append(code);
            block.append(pre);
            fragment.append(block);
            continue;
        }

        const heading = line.match(/^(#{1,6})\s+(.+)$/);
        if (heading) {
            const level = Math.min(heading[1].length + 1, 6);
            const element = document.createElement(`h${level}`);
            appendInlineMarkdown(element, heading[2].trim());
            fragment.append(element);
            index++;
            continue;
        }

        if (/^\s*(---+|___+|\*\*\*+)\s*$/.test(line)) {
            fragment.append(document.createElement('hr'));
            index++;
            continue;
        }

        if (index + 1 < lines.length && line.includes('|') && isTableDivider(lines[index + 1])) {
            const tableWrap = document.createElement('div');
            tableWrap.className = 'markdown-table-wrap';
            const table = document.createElement('table');
            const thead = document.createElement('thead');
            const headRow = document.createElement('tr');
            tableCells(line).forEach(value => {
                const cell = document.createElement('th');
                appendInlineMarkdown(cell, value);
                headRow.append(cell);
            });
            thead.append(headRow);
            table.append(thead);
            index += 2;
            const tbody = document.createElement('tbody');
            while (index < lines.length && lines[index].includes('|') && lines[index].trim()) {
                const row = document.createElement('tr');
                tableCells(lines[index]).forEach(value => {
                    const cell = document.createElement('td');
                    appendInlineMarkdown(cell, value);
                    row.append(cell);
                });
                tbody.append(row);
                index++;
            }
            table.append(tbody);
            tableWrap.append(table);
            fragment.append(tableWrap);
            continue;
        }

        if (/^\s*>\s?/.test(line)) {
            const quote = document.createElement('blockquote');
            while (index < lines.length && /^\s*>\s?/.test(lines[index])) {
                if (quote.childNodes.length) quote.append(document.createElement('br'));
                appendInlineMarkdown(quote, lines[index].replace(/^\s*>\s?/, ''));
                index++;
            }
            fragment.append(quote);
            continue;
        }

        const unordered = line.match(/^\s*[-+*]\s+(.+)$/);
        const ordered = line.match(/^\s*\d+[.)]\s+(.+)$/);
        if (unordered || ordered) {
            const list = document.createElement(ordered ? 'ol' : 'ul');
            const matcher = ordered ? /^\s*\d+[.)]\s+(.+)$/ : /^\s*[-+*]\s+(.+)$/;
            while (index < lines.length) {
                const itemMatch = lines[index].match(matcher);
                if (!itemMatch) break;
                const item = document.createElement('li');
                appendInlineMarkdown(item, itemMatch[1]);
                list.append(item);
                index++;
            }
            fragment.append(list);
            continue;
        }

        const paragraph = document.createElement('p');
        while (index < lines.length && lines[index].trim()) {
            const nextLine = lines[index];
            if (paragraph.childNodes.length) paragraph.append(document.createElement('br'));
            appendInlineMarkdown(paragraph, nextLine.trim());
            index++;
            if (index < lines.length && (
                /^(#{1,6})\s+/.test(lines[index]) ||
                /^\s*```/.test(lines[index]) ||
                /^\s*>\s?/.test(lines[index]) ||
                /^\s*([-+*]|\d+[.)])\s+/.test(lines[index])
            )) break;
        }
        fragment.append(paragraph);
    }
    return fragment;
}

function renderChat(data) {
    elements.chatOutput.className = 'output-panel';
    elements.chatOutput.replaceChildren();
    const head = document.createElement('div');
    head.className = 'result-head';
    const title = document.createElement('h3');
    title.textContent = data.knowledgeFound ? '基于知识库的回答' : '未找到相关资料';
    const meta = document.createElement('span');
    meta.textContent = data.sources?.length ? `${data.sources.length} 条引用` : '无引用';
    head.append(title, meta);
    const body = document.createElement('div');
    body.className = data.knowledgeFound ? 'answer-body markdown-body' : 'notice';
    if (data.knowledgeFound) {
        body.append(renderMarkdown(data.answer || '模型未返回回答。'));
    } else {
        body.textContent = data.answer || '模型未返回回答。';
    }
    elements.chatOutput.append(head, body);
    if (data.sources?.length) {
        const heading = document.createElement('h4');
        heading.className = 'source-heading';
        heading.textContent = '引用来源';
        const list = document.createElement('div');
        list.className = 'source-list';
        data.sources.forEach(source => list.append(hitCard(source, source.referenceId)));
        elements.chatOutput.append(heading, list);
    }
    if (data.tokenUsage) {
        const usage = document.createElement('div');
        usage.className = 'usage';
        usage.textContent = `Token：输入 ${data.tokenUsage.promptTokens ?? '-'} / 输出 ${data.tokenUsage.completionTokens ?? '-'} / 总计 ${data.tokenUsage.totalTokens ?? '-'}`;
        elements.chatOutput.append(usage);
    }
}

elements.createToggle.addEventListener('click', () => {
    const open = elements.createForm.hidden;
    elements.createForm.hidden = !open;
    elements.createToggle.setAttribute('aria-expanded', String(open));
    if (open) document.querySelector('#kb-name').focus();
});

document.querySelector('#create-cancel').addEventListener('click', () => {
    elements.createForm.hidden = true;
    elements.createToggle.setAttribute('aria-expanded', 'false');
});

elements.createForm.addEventListener('submit', async event => {
    event.preventDefault();
    const button = event.submitter;
    await withBusy(button, async () => {
        const form = new FormData(elements.createForm);
        await api('/api/knowledge-bases', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: form.get('name').trim(), description: form.get('description').trim() })
        });
        const name = form.get('name').trim();
        elements.createForm.reset();
        elements.createForm.hidden = true;
        elements.createToggle.setAttribute('aria-expanded', 'false');
        await loadLibraries();
        const created = state.libraries.find(item => item.name === name);
        if (created) await selectLibrary(created.id);
        notify('知识库已创建');
    }).catch(error => notify(error.message, 'error'));
});

document.querySelector('#refresh-libraries').addEventListener('click', () => loadLibraries());
document.querySelectorAll('.view-tab').forEach(tab => tab.addEventListener('click', () => switchView(tab.dataset.view)));

elements.fileInput.addEventListener('change', () => {
    elements.fileName.textContent = elements.fileInput.files[0]?.name || '选择 TXT 或 Markdown 文件';
});

document.querySelector('#upload-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (!requireLibrary()) return;
    const file = elements.fileInput.files[0];
    if (!file) return notify('请选择要上传的文件', 'error');
    const button = event.submitter;
    await withBusy(button, async () => {
        const form = new FormData();
        form.append('file', file);
        const uploaded = await api(`/api/knowledge-bases/${state.activeLibrary.id}/documents`, { method: 'POST', body: form });
        elements.documentId.value = uploaded.id;
        persistDocument(uploaded);
        renderDocument(uploaded);
        notify(`“${uploaded.originalName}”已上传并解析`);
    }).catch(error => notify(error.message, 'error'));
});

elements.documentId.addEventListener('input', () => renderDocument(currentDocument()));

elements.chunkButton.addEventListener('click', async () => {
    if (!requireLibrary()) return;
    const document = currentDocument();
    if (!document) return notify('请填写文档 ID', 'error');
    await withBusy(elements.chunkButton, async () => {
        const count = await api(`/api/knowledge-bases/${state.activeLibrary.id}/documents/${encodeURIComponent(document.id)}/chunks`, { method: 'POST' });
        const updated = { ...document, status: 'CHUNKED', chunkCount: count };
        persistDocument(updated);
        renderDocument(updated);
        notify(`文本切分完成，共 ${count} 个 Chunk`);
    }).catch(error => notify(error.message, 'error'));
});

elements.indexButton.addEventListener('click', async () => {
    if (!requireLibrary()) return;
    const document = currentDocument();
    if (!document) return notify('请填写文档 ID', 'error');
    await withBusy(elements.indexButton, async () => {
        const indexed = await api(`/api/knowledge-bases/${state.activeLibrary.id}/documents/${encodeURIComponent(document.id)}/index`, { method: 'POST' });
        const updated = { ...document, status: indexed.status, vectorCount: indexed.vectorCount, embeddingModel: indexed.embeddingModel };
        persistDocument(updated);
        renderDocument(updated);
        notify(`向量索引完成，共 ${indexed.vectorCount} 条`);
    }).catch(error => notify(error.message, 'error'));
});

document.querySelector('#retrieval-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (!requireLibrary()) return;
    const form = new FormData(event.currentTarget);
    const button = event.submitter;
    await withBusy(button, async () => {
        const data = await api('/api/retrieval/search', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                knowledgeBaseId: state.activeLibrary.id,
                query: form.get('query').trim(),
                topK: Number(form.get('topK')),
                similarityThreshold: Number(form.get('similarityThreshold'))
            })
        });
        renderRetrieval(data);
    }).catch(error => notify(error.message, 'error'));
});

document.querySelector('#chat-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (!requireLibrary()) return;
    const form = new FormData(event.currentTarget);
    const button = event.submitter;
    await withBusy(button, async () => {
        const data = await api('/api/rag/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                knowledgeBaseId: state.activeLibrary.id,
                question: form.get('question').trim(),
                topK: Number(form.get('topK')),
                similarityThreshold: Number(form.get('similarityThreshold'))
            })
        });
        renderChat(data);
    }).catch(error => notify(error.message, 'error'));
});

document.querySelector('#chat-question').addEventListener('keydown', event => {
    if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
        document.querySelector('#chat-form').requestSubmit();
    }
});

updateDocumentActions();
loadLibraries();
