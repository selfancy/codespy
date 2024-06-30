package com.selfancy.plugin.utils;

import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.selfancy.plugin.bundle.CodeSpyBundle;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.function.Function;

/**
 * {@link JarUtils}
 * <p>
 * Created by mike on 2024/05/18
 */
@UtilityClass
public final class JarUtils {
    private static final Function<String, String> DECOMPILED_METADATA_FILENAME = sourceJarName ->
            CodeSpyBundle.message("decompiler.decompiled.metadata.file.name", sourceJarName);

    @SneakyThrows
    public void writeMetadataFile(File sourceJarFile) {
        if (sourceJarFile.exists()) {
            String metadataFilename = DECOMPILED_METADATA_FILENAME.apply(sourceJarFile.getName());
            Files.writeString(Paths.get(sourceJarFile.getParent(), metadataFilename), sourceJarFile.getAbsolutePath(),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        }
    }

    public boolean existsSourceMetadataFile(VirtualFile virtualFile) {
        String filename = virtualFile.getName();
        if (!filename.endsWith("-sources.jar")) {
            filename = virtualFile.getNameWithoutExtension() + "-sources.jar";
        }
        File jarFile = VfsUtil.virtualToIoFile(virtualFile);
        String metadataFilename = DECOMPILED_METADATA_FILENAME.apply(filename);
        return Files.exists(Paths.get(jarFile.getParent(), metadataFilename));
    }

    public String getJarSourcesFilename(File jarFile) {
        String filename = jarFile.getName();
        return filename.substring(0, filename.lastIndexOf(".")) + "-sources.jar";
    }

    public void attachSourceJar(Library library) {
        VirtualFile jarFile = library.getFiles(OrderRootType.CLASSES)[0];
        File file = VfsUtil.virtualToIoFile(jarFile);
        String jarSourcesFilename = JarUtils.getJarSourcesFilename(file);
        VirtualFile srcFile = LocalFileSystem.getInstance()
                .refreshAndFindFileByIoFile(new File(file.getParent(), jarSourcesFilename));
        if (srcFile == null) return;

        VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(srcFile);
        if (jarRoot == null) return;
        WriteAction.run(() -> {
            Library.ModifiableModel model = library.getModifiableModel();
            Arrays.stream(library.getRootProvider().getUrls(OrderRootType.SOURCES))
                    .filter(sourceUrl -> sourceUrl.endsWith("-sources.jar!/"))
                    .forEach(sourceUrl -> model.removeRoot(sourceUrl, OrderRootType.SOURCES));
            model.addRoot(jarRoot, OrderRootType.SOURCES);
            model.commit();

            FileDocumentManager.getInstance().saveAllDocuments();
            SaveAndSyncHandler.getInstance().refreshOpenFiles();
        });
    }
}
