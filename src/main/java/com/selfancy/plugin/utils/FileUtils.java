package com.selfancy.plugin.utils;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * {@link FileUtils}
 * <p>
 * Created by mike on 2024/05/18
 */
@UtilityClass
public class FileUtils {

    public static final String CLASS_FILE_TYPE = "CLASS";
    public static final String JAVA_FILE_TYPE = "JAVA";
    public static final String JAR_FILE_EXTENSION = "jar";

    public boolean isClassFile(VirtualFile file) {
        return isClassFile(file.getFileType());
    }

    public boolean isClassFile(FileType fileType) {
        return CLASS_FILE_TYPE.equals(fileType.getName());
    }

    public boolean isJavaFile(VirtualFile file) {
        return isJavaFile(file.getFileType());
    }

    public boolean isJavaFile(FileType fileType) {
        return JAVA_FILE_TYPE.equals(fileType.getName());
    }

    public File getSourceFile(VirtualFile classFile) {
        String parentPath = classFile.getParent().toNioPath().toAbsolutePath().toString();
        return new File(parentPath, classFile.getNameWithoutExtension() + ".java");
    }

    @SneakyThrows
    public void deleteFile(File file) {
        Path filePath = file.toPath();
        if (Files.exists(filePath) && Files.isDirectory(filePath)) {
            try (Stream<Path> fileStream = Files.list(filePath)) {
                fileStream.forEach(path -> deleteFile(path.toFile()));
            }
        }
        Files.deleteIfExists(filePath);
    }
}
