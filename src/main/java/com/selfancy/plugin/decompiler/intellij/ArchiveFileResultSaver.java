package com.selfancy.plugin.decompiler.intellij;

import com.selfancy.plugin.utils.FileUtils;
import com.selfancy.plugin.utils.JarUtils;
import lombok.RequiredArgsConstructor;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * {@link ArchiveFileResultSaver}
 * <p>
 * Created by mike on 2024/05/18
 *
 * @see IResultSaverEx
 */
@RequiredArgsConstructor
final class ArchiveFileResultSaver implements IResultSaverEx {
    private final Set<String> archiveEntries = new HashSet<>();
    private ZipOutputStream archiveStream;
    private final String outputArchivePath;

    ArchiveFileResultSaver(FileInfo file) {
        File sourceJarFile = new File(file.toFile().getParent() + "/"
                + JarUtils.getJarSourcesFilename(file.toFile()));
        this.outputArchivePath = sourceJarFile.getAbsolutePath();
        FileUtils.deleteFile(new File(outputArchivePath));
    }

    @Override
    public void close() {
        JarUtils.writeMetadataFile(new File(outputArchivePath));
    }

    private String getOutputArchivePath() {
        return outputArchivePath;
    }

    @Override
    public void saveFolder(String path) {
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
        try {
            InterpreterUtil.copyFile(new File(source), new File(getOutputArchivePath(), entryName));
        } catch (IOException ex) {
            DecompilerContext.getLogger().writeMessage("Cannot copy " + source + " to " + entryName, ex);
        }
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {

    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {
        File file = new File(getOutputArchivePath());
        try {
            if (!(file.createNewFile() || file.isFile())) {
                throw new IOException("Cannot create file " + file);
            }
            FileOutputStream fileStream = new FileOutputStream(file);
            this.archiveStream = manifest != null ? new JarOutputStream(fileStream, manifest) : new ZipOutputStream(fileStream);
        } catch (IOException ex) {
            DecompilerContext.getLogger().writeMessage("Cannot create archive " + file, ex);
        }
    }

    @Override
    public void saveDirEntry(String path, String archiveName, String entryName) {
        saveClassEntry(null, null, null, entryName, null);
    }

    @Override
    public void copyEntry(String source, String path, String archiveName, String entryName) {
        String file = getOutputArchivePath();
        if (!checkEntry(entryName, file)) {
            return;
        }
        try (ZipFile srcArchive = new ZipFile(new File(source))) {
            ZipEntry entry = srcArchive.getEntry(entryName);
            if (entry != null) {
                try (InputStream in = srcArchive.getInputStream(entry)) {
                    ZipOutputStream out = this.archiveStream;
                    out.putNextEntry(new ZipEntry(entryName));
                    InterpreterUtil.copyStream(in, out);
                }
            }
        } catch (IOException ex) {
            String message = "Cannot copy entry " + entryName + " from " + source + " to " + file;
            DecompilerContext.getLogger().writeMessage(message, ex);
        }
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
        String file = getOutputArchivePath();
        if (!checkEntry(entryName, file)) {
            return;
        }
        try {
            ZipOutputStream out = this.archiveStream;
            out.putNextEntry(new ZipEntry(entryName));
            if (content != null) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            String message = "Cannot write entry " + entryName + " to " + file;
            DecompilerContext.getLogger().writeMessage(message, ex);
        }
    }

    @Override
    public void closeArchive(String path, String archiveName) {
        String file = getOutputArchivePath();
        try {
            this.archiveStream.finish();
            this.archiveStream.close();
        } catch (IOException ex) {
            DecompilerContext.getLogger().writeMessage("Cannot close " + file, IFernflowerLogger.Severity.WARN);
        }
    }

    @SuppressWarnings("all")
    private boolean checkEntry(String entryName, String file) {
        Set<String> set = this.archiveEntries;
        boolean added = set.add(entryName);
        if (!added) {
            String message = "Zip entry " + entryName + " already exists in " + file;
            DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
        }
        return added;
    }
}
