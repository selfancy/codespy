package com.selfancy.plugin.decompiler.intellij;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.selfancy.plugin.utils.FileUtils;
import lombok.Getter;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * {@link FileInfo}
 * <p>
 * Created by mike on 2024/05/18
 *
 * @see IBytecodeProvider
 */
public class FileInfo implements IBytecodeProvider {
    @Getter
    private final VirtualFile file;
    @Getter
    private final boolean classFile;
    private Map<String, VirtualFile> innerClassMap;

    public FileInfo(VirtualFile file) {
        this.file = file;
        this.classFile = FileUtils.isClassFile(file);
        if (classFile) {
            String mask = file.getNameWithoutExtension() + "$";
            this.innerClassMap = Stream.concat(Stream.of(file), Arrays.stream(file.getParent().getChildren())
                            .filter(it -> it.getName().startsWith(mask) && FileUtils.isClassFile(it)))
                    .collect(Collectors.toMap(VirtualFile::getPath, Function.identity()));
        }
    }

    @Override
    public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
        if (classFile) {
            VirtualFile file = innerClassMap.get(externalPath);
            if (file == null) {
                throw new AssertionError(String.format("%s not in %s", externalPath, innerClassMap.keySet()));
            }
            return file.contentsToByteArray(false);
        }
        try (ZipFile archive = new ZipFile(externalPath)) {
            ZipEntry entry = archive.getEntry(internalPath);
            if (entry == null) throw new IOException("Entry not found: " + internalPath);
            return archive.getInputStream(entry).readAllBytes();
        }
    }

    public Collection<File> getClassFiles() {
        if (classFile) {
            return innerClassMap.values().stream()
                    .map(VfsUtil::virtualToIoFile)
                    .toList();
        }
        throw new AssertionError("not class file");
    }

    public File toFile() {
        return VfsUtil.virtualToIoFile(file);
    }
}
