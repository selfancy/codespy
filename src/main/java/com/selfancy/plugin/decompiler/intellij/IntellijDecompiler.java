package com.selfancy.plugin.decompiler.intellij;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.selfancy.plugin.decompiler.InternalDecompiler;
import lombok.RequiredArgsConstructor;
import lombok.experimental.StandardException;
import org.jetbrains.java.decompiler.main.CancellationManager;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.extern.ClassFormatException;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;

/**
 * {@link IntellijDecompiler}
 * <p>
 * Created by mike on 2024/05/18
 *
 * @see InternalDecompiler
 */
@RequiredArgsConstructor
public class IntellijDecompiler implements InternalDecompiler {

    private static final String BANNER = "//\n// Source code recreated from a .class file by IntelliJ IDEA\n// (powered by FernFlower decompiler)\n//\n\n";

    @Override
    @SuppressWarnings("all")
    public void decompile(VirtualFile file) {
        Map<String, Object> options = new HashMap<>(getOptions());
        if (Registry.is("decompiler.use.line.mapping")) {
            options.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
        }
        if (Registry.is("decompiler.dump.original.lines")) {
            options.put(IFernflowerPreferences.DUMP_ORIGINAL_LINES, "1");
        }
        int maxMethodTimeoutSec = (int) options.getOrDefault(IFernflowerPreferences.MAX_PROCESSING_METHOD, 0);
        FileInfo fileInfo = new FileInfo(file);
        if (fileInfo.isClassFile()) {
            if (!file.isInLocalFileSystem()) {
                options.put("JAR_FILE", VfsUtil.virtualToIoFile(JarFileSystem.getInstance()
                        .getLocalVirtualFileFor(file)));
            }
        } else {
            options.put("JAR_FILE", VfsUtil.virtualToIoFile(file));
        }
        try {
            try (IResultSaverEx saver = getResultSaver(fileInfo)) {
                BaseDecompiler decompiler = new BaseDecompiler(
                        fileInfo, saver, options, DecompileLogger.INSTANCE,
                        new IdeaCancellationManager(maxMethodTimeoutSec));
                if (fileInfo.isClassFile()) {
                    fileInfo.getClassFiles().forEach(decompiler::addSource);
                } else {
                    DecompilerContext.getStructContext().addSpace(fileInfo.toFile(), true);
                }
                decompiler.decompileContext();
            }
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof ProcessCanceledException) {
                throw (ProcessCanceledException) cause;
            }
            if (e instanceof DecompileLogger.InternalException && cause instanceof IOException) {
                Logger.getInstance(DecompileLogger.class).warn(file.getUrl(), e);
            } else {
                if (ApplicationManager.getApplication().isUnitTestMode() && e instanceof ClassFormatException) {
                    throw new AssertionError(file.getUrl(), e);
                }
                throw new CannotDecompileException(file.getUrl(), e);
            }
        }
    }

    protected IResultSaverEx getResultSaver(FileInfo file) {
        return file.isClassFile() ? new ClassFileResultSaver(file) : new ArchiveFileResultSaver(file);
    }

    private Map<String, Object> getOptions() {
        CommonCodeStyleSettings.IndentOptions options = CodeStyle.getDefaultSettings().getLanguageIndentOptions(
                Objects.requireNonNull(Language.findLanguageByID("JAVA")));
        String indent = StringUtil.repeat(" ", options.INDENT_SIZE);
        return Map.ofEntries(
                entry(IFernflowerPreferences.HIDE_DEFAULT_CONSTRUCTOR, "0"),
                entry(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1"),
                entry(IFernflowerPreferences.REMOVE_SYNTHETIC, "1"),
                entry(IFernflowerPreferences.REMOVE_BRIDGE, "1"),
                entry(IFernflowerPreferences.NEW_LINE_SEPARATOR, "1"),
                entry(IFernflowerPreferences.BANNER, BANNER),
                entry(IFernflowerPreferences.MAX_PROCESSING_METHOD, 60),
                entry(IFernflowerPreferences.INDENT_STRING, indent),
                entry(IFernflowerPreferences.IGNORE_INVALID_BYTECODE, "1"),
                entry(IFernflowerPreferences.VERIFY_ANONYMOUS_CLASSES, "1"),
                entry(IFernflowerPreferences.UNIT_TEST_MODE, ApplicationManager.getApplication().isUnitTestMode() ? "1" : "0"));
    }

    @StandardException
    private static class CannotDecompileException extends RuntimeException {
    }

    @SuppressWarnings("all")
    class IdeaCancellationManager extends CancellationManager.TimeoutCancellationManager {

        public IdeaCancellationManager(int maxMethodTimeoutSec) {
            super(maxMethodTimeoutSec);
        }

        @Override
        public void checkCanceled() throws CanceledException {
            try {
                ProgressManager.checkCanceled();
            } catch (ProcessCanceledException e) {
                throw new CanceledException(e);
            }
            super.checkCanceled();
        }
    }
}
