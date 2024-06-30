package com.selfancy.plugin.debug;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.PositionManagerFactory;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.PositionManagerImpl;
import com.intellij.debugger.impl.SourceCodeChecker;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.selfancy.plugin.decompiler.DecompiledResult;
import com.selfancy.plugin.utils.DecompileUtils;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * {@link DebugPositionManagerFactory}
 * <p>
 * Created by mike on 2024/05/18
 *
 * @see PositionManagerFactory
 */
final class DebugPositionManagerFactory extends PositionManagerFactory {

    private record SourceFiles(PsiFile targetFile, PsiFile classFile) {
    }

    @Override
    public PositionManager createPositionManager(@NotNull DebugProcess debugProcess) {
        return new PositionManagerImpl((DebugProcessImpl) debugProcess) {
            private static final ThreadLocal<SourceFiles> SOURCE_FILES = new ThreadLocal<>();

            @Override
            protected @Nullable PsiFile getPsiFileByLocation(Project project, Location location) {
                PsiFile psiFile = super.getPsiFileByLocation(project, location);
                if (psiFile != null) {
                    DecompiledResult result = saveMappingIfAvailable(psiFile);
                    PsiFile classPsiFile = result.getClassPsiFile(project);
                    if (result.hasMapping() && classPsiFile != null) {
                        SOURCE_FILES.set(new SourceFiles(psiFile, classPsiFile));
                        return classPsiFile;
                    }
                }
                return psiFile;
            }

            @Override
            public SourcePosition getSourcePosition(Location location) throws NoDataException {
                try {
                    SourcePosition position = super.getSourcePosition(location);
                    if (position == null) {
                        return null;
                    }
                    SourceFiles sourceFiles = SOURCE_FILES.get();
                    if (sourceFiles != null) {
                        return new SkipSourceCodeCheckSourcePosition(position, sourceFiles);
                    }
                    return position;
                } finally {
                    SOURCE_FILES.remove();
                }
            }

            @Override
            public @NotNull List<ClassPrepareRequest> createPrepareRequests(@NotNull ClassPrepareRequestor requester, @NotNull SourcePosition position) throws NoDataException {
                saveMappingIfAvailable(position.getFile());
                return super.createPrepareRequests(requester, position);
            }

            private DecompiledResult saveMappingIfAvailable(PsiFile file) {
                DecompiledResult result = DecompileUtils.decompileWithPreCheck(debugProcess.getProject(), file);
                result.saveMappingIfDecompiled();
                return result;
            }
        };
    }

    private static class SkipSourceCodeCheckSourcePosition extends SourcePositionWrapper {
        private final SourcePosition target;
        private final PsiFile classFile;

        SkipSourceCodeCheckSourcePosition(SourcePosition delegate, SourceFiles sourceFiles) {
            super(delegate);
            this.classFile = sourceFiles.classFile;
            this.target = SourcePosition.createFromLine(sourceFiles.targetFile, delegate.getLine());
        }

        @Override
        public @NotNull PsiFile getFile() {
            StackTraceElement stackTrace = Thread.currentThread().getStackTrace()[2];
            if (stackTrace.getClassName().equals(SourceCodeChecker.class.getName())) {
                // skip source code check, use class file, see: Source code does not match the bytecode
                return classFile;
            }
            return target.getFile();
        }

        @Override
        public PsiElement getElementAt() {
            return target.getElementAt();
        }
    }
}
