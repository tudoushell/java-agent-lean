package com.elliot.ai.rag.parse;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.StorageProperties;
import com.elliot.ai.rag.dto.ParsedText;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.UUID;

public abstract class AbstractDocumentParse {
    private final Path uploadRoot;
    private final Path parseRoot;
    private static final int PREVIEW_LENGTH = 500;


    public AbstractDocumentParse(StorageProperties properties) {
        this.uploadRoot = Paths.get(properties.getRootDirectory())
                .toAbsolutePath().normalize();
        this.parseRoot = Paths.get(properties.getParsedDirectory())
                .toAbsolutePath().normalize();
    }

    public abstract boolean isSupport(String extension);

    protected abstract String parseFile(Path sourcePath) throws IOException;

    /**
     * 解析已上传的文件，将文本内容保存到解析目录，并返回解析文件路径、预览内容和文本长度。
     *
     * @param documentId 文档唯一标识，用于生成解析结果文件名
     * @param storagePath 上传目录下的文件相对路径
     * @return 解析后的文本信息
     */
    public ParsedText parse(UUID documentId, String storagePath) {
        Path sourcePath = resolveSafely(uploadRoot, storagePath);
        try {
            String content = parseFile(sourcePath);
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ResultCode.FAIL, "文件内容为空");
            }
            String parsedRelativePath = buildParsedPath(documentId);
            Path targetPath = resolveSafely(parseRoot, parsedRelativePath);
            Files.createDirectories(targetPath.getParent());
            Files.writeString(
                    targetPath,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            return new ParsedText(
                    parsedRelativePath,
                    createPreview(content),
                    content.length()
            );
        } catch (IOException e) {
            throw new BusinessException(
                    ResultCode.FAIL,
                    "文档解析失败：" + e.getMessage()
            );
        }
    }

    private String createPreview(String content) {
        if (content.length() <= PREVIEW_LENGTH) {
            return content;
        }

        return content.substring(0, PREVIEW_LENGTH);
    }


    private String buildParsedPath(UUID documentId) {
        LocalDate today = LocalDate.now();

        return Paths.get(
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                documentId + ".txt"
        ).toString();
    }

    protected String normalize(String content) {
        // 删除 UTF-8 BOM
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        // 统一换行符
        return content
                .replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    private Path resolveSafely(Path root, String storagePath) {
        Path target = root.resolve(storagePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ResultCode.FAIL, "文件路径不合法");
        }
        return target;
    }
}
