package com.selfancy.plugin.decompiler.intellij;

import com.selfancy.plugin.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * {@link ClassFileResultSaver}
 * <p>
 * Created by mike on 2024/05/18
 *
 * @see LineMappingResultSaver
 */
@RequiredArgsConstructor
final class ClassFileResultSaver extends LineMappingResultSaver {

    private final FileInfo file;

    @Override
    public void close() throws IOException {
        if (file.getFile().isInLocalFileSystem()) {
            File sourceFile = FileUtils.getSourceFile(file.getFile());
            Files.writeString(sourceFile.toPath(), result.content(), StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        }
    }
}
