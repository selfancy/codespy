package com.selfancy.plugin.decompiler.intellij;

import com.intellij.execution.filters.LineNumbersMapping;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.selfancy.plugin.Decompiler;
import com.selfancy.plugin.decompiler.DecompiledResult;
import com.selfancy.plugin.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

/**
 * {@link IdeaDecompilerExtends}
 * <p>
 * Created by mike on 2024/05/19
 *
 * @see ClassFileDecompilers.Light
 */
final class IdeaDecompilerExtends extends ClassFileDecompilers.Light {
    private static final ThreadLocal<LineMappingResultSaver> RESULT_SAVER = new ThreadLocal<>();
    private static final Decompiler DECOMPILER = new IntellijDecompiler() {
        @Override
        protected IResultSaverEx getResultSaver(FileInfo file) {
            return RESULT_SAVER.get();
        }
    };

    @Override
    public @NotNull CharSequence getText(@NotNull VirtualFile file) throws CannotDecompileException {
        try (LineMappingResultSaver resultSaver = new LineMappingResultSaver()) {
            RESULT_SAVER.set(resultSaver);
            DECOMPILER.decompile(file);
            IResultSaverEx.Result result = resultSaver.getResult();
            if (result != null) {
                file.putUserData(DecompiledResult.IS_MY_DECOMPILED, Boolean.TRUE);
                file.putUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY,
                        new LineNumbersMapping.ArrayBasedMapping(result.mapping()));
                return result.content();
            }
            throw new RuntimeException("Decompile with none result");
        } catch (Throwable e) {
            throw new CannotDecompileException("IdeaDecompilerExtends", e);
        } finally {
            RESULT_SAVER.remove();
        }
    }

    @Override
    public boolean accepts(@NotNull VirtualFile file) {
        return FileUtils.isClassFile(file);
    }
}
