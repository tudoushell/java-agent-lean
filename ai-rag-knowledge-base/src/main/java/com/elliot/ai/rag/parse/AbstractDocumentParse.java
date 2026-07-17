package com.elliot.ai.rag.parse;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.StorageProperties;
import com.elliot.ai.rag.dto.ParsedResult;
import com.elliot.ai.rag.dto.ParsedText;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
public abstract class AbstractDocumentParse {
    private final Path uploadRoot;
    private final Path parseRoot;
    protected static final int PREVIEW_LENGTH = 500;


    public AbstractDocumentParse(StorageProperties properties) {
        this.uploadRoot = Paths.get(properties.getRootDirectory())
                .toAbsolutePath().normalize();
        this.parseRoot = Paths.get(properties.getParsedDirectory())
                .toAbsolutePath().normalize();
    }

    public abstract boolean isSupport(String extension);

    /**
     * 读取文件内容，将文件写入解析后的目录
     *
     * @param sourcePath
     * @param targetPath
     * @return
     * @throws IOException
     */
    protected abstract ParsedResult  parseFile(Path sourcePath, Path targetPath) throws IOException;

    /**
     * 解析已上传的文件，将文本内容保存到解析目录，并返回解析文件路径、预览内容和文本长度。
     *
     * @param documentId  文档唯一标识，用于生成解析结果文件名
     * @param storagePath 上传目录下的文件相对路径
     * @return 解析后的文本信息
     */
    public ParsedText parse(UUID documentId, String storagePath) {
        Path sourcePath = resolveSafely(uploadRoot, storagePath);
        String parsedRelativePath = buildParsedPath(documentId);
        Path targetPath = resolveSafely(parseRoot, parsedRelativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            ParsedResult parsedResult = parseFile(sourcePath, targetPath);
            if (!parsedResult.isHasText()) {
                throw new BusinessException(ResultCode.FAIL, "文件内容为空");
            }
            return new ParsedText(parsedRelativePath, parsedResult.getLimitContent(), parsedResult.getCharCount());
        } catch (BusinessException e) {
            deleteTargetFile(targetPath);
            throw e;
        } catch (IOException e) {
            deleteTargetFile(targetPath);
            throw new BusinessException(
                    ResultCode.FAIL,
                    "文档解析失败：" + e.getMessage()
            );
        }
    }

    private void deleteTargetFile(Path targetPath) {
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.error("删除解析失败文件失败：{}", targetPath, e);
        }
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

    protected boolean isUTF8BOM(char content) {
        return content == '\uFEFF';
    }

    private Path resolveSafely(Path root, String storagePath) {
        Path target = root.resolve(storagePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ResultCode.FAIL, "文件路径不合法");
        }
        return target;
    }
}
