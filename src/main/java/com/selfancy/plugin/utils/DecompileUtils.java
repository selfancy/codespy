package com.selfancy.plugin.utils;

import com.intellij.execution.filters.LineNumbersMapping;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.selfancy.plugin.Decompiler;
import com.selfancy.plugin.decompiler.DecompiledResult;
import com.selfancy.plugin.decompiler.intellij.FileInfo;
import com.selfancy.plugin.decompiler.intellij.IResultSaverEx;
import com.selfancy.plugin.decompiler.intellij.IntellijDecompiler;
import com.selfancy.plugin.decompiler.intellij.LineMappingResultSaver;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import static com.selfancy.plugin.bundle.CodeSpyBundle.message;

/**
 * {@link DecompileUtils}
 * <p>
 * Created by mike on 2024/05/18
 */
@UtilityClass
public class DecompileUtils {
    private static final Logger LOG = Logger.getInstance(DecompileUtils.class);
//    private static final LineMappingProvider lineMappingProvider = BinaryFileDecompilerProvider.INSTANCE;
    private static final LineMappingProvider lineMappingProvider = IntellijDecompilerProvider.INSTANCE;

    public DecompiledResult decompileWithPreCheck(Project project, PsiFile psiFile) {
        return ReadAction.compute(() -> {
            if (psiFile instanceof PsiJavaFile psiJavaFile && !isModuleInfoOrPackageInfo(psiFile)
                    && !isJdkClass(psiJavaFile)) {
                VirtualFile file = psiJavaFile.getVirtualFile();
                if (!(FileUtils.isJavaFile(file) || FileUtils.isClassFile(file))) {
                    return DecompiledResult.INVALID;
                }
                boolean isJavaFile = FileUtils.isJavaFile(file);
                ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
                if (isJavaFile) {
                    if (!fileIndex.isInLibrarySource(file)) {
                        return DecompiledResult.INVALID;
                    }
                } else {
                    if (!fileIndex.isInLibraryClasses(file)) {
                        return DecompiledResult.INVALID;
                    }
                }
                if (!isMyDecompiledFile(fileIndex, file)) {
                    return DecompiledResult.INVALID;
                }
                VirtualFile classFile = isJavaFile ? findLibraryClassFileByJava(fileIndex, file) : file;
                if (classFile == null) {
                    return DecompiledResult.INVALID;
                }
                VirtualFile javaFile = isJavaFile ? file : findLibraryJavaFileByClass(fileIndex, file);
                if (javaFile == null) {
                    return DecompiledResult.INVALID;
                }
                if (existsLineMapping(javaFile)) {
                    return new DecompiledResult(javaFile, classFile);
                }
                if (existsLineMapping(classFile)) {
                    return new DecompiledResult(javaFile, classFile);
                }
                if (classFile.getUserData(DecompiledResult.IS_MY_DECOMPILED) == null) {
                    DecompiledResult result = decompile(classFile);
                    result.setJavaFile(javaFile);
                    return result;
                }
            }
            return DecompiledResult.INVALID;
        });
    }

    public DecompiledResult decompile(VirtualFile classFile) {
        try {
            DecompiledResult result = lineMappingProvider.getDecompiled(classFile);
            result.setClassFile(classFile);
            return result;
        } catch (Throwable e) {
            if (!(e instanceof ProcessCanceledException)) {
                LOG.error(message("decompiler.notification.decompiling.file.error",
                        classFile.getCanonicalPath(), e.getMessage()), e);
            }
            return DecompiledResult.EXCEPTION;
        }
    }

    public boolean isModuleInfoOrPackageInfo(PsiFile psiFile) {
        String filename = psiFile.getVirtualFile().getNameWithoutExtension();
        return filename.equals("module-info") || filename.equals("package-info");
    }

    private boolean isJdkClass(PsiJavaFile javaFile) {
        String className = javaFile.getClasses()[0].getQualifiedName();
        if (className != null) {
            try {
                return Class.forName(className).getClassLoader() == null;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean isMyDecompiledFile(ProjectFileIndex fileIndex, VirtualFile file) {
        VirtualFile jarDir = null;
        if (FileUtils.isJavaFile(file)) {
            jarDir = fileIndex.getSourceRootForFile(file);
        } else if (FileUtils.isClassFile(file)) {
            jarDir = fileIndex.getClassRootForFile(file);
        }
        return jarDir != null && JarUtils.existsSourceMetadataFile(jarDir);
    }

    public boolean existsLineMapping(VirtualFile file) {
        return file.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY) != null
                && Boolean.TRUE.equals(file.getUserData(DecompiledResult.IS_MY_DECOMPILED));
    }

    @SuppressWarnings("all")
    public VirtualFile findLibraryClassFileByJava(ProjectFileIndex fileIndex, VirtualFile javaFile) {
        try {
            VirtualFile sourceRootForFile = fileIndex.getSourceRootForFile(javaFile);
            String url = sourceRootForFile.getUrl();
            String jarPath = url.substring(JarFileSystem.PROTOCOL_PREFIX.length(), url.lastIndexOf("-sources.jar")) + ".jar!/";
            String entryFilePath = StringUtil.trimEnd(VfsUtilCore.getRelativeLocation(javaFile, sourceRootForFile), ".java") + ".class/";
            return JarFileSystem.getInstance().findFileByPathIfCached(jarPath + entryFilePath);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("all")
    private VirtualFile findLibraryJavaFileByClass(ProjectFileIndex fileIndex, VirtualFile classFile) {
        try {
            VirtualFile classRootForFile = fileIndex.getClassRootForFile(classFile);
            String url = classRootForFile.getUrl();
            String jarPath = url.substring(JarFileSystem.PROTOCOL_PREFIX.length(), url.lastIndexOf(".jar")) + "-sources.jar!/";
            String entryFilePath = StringUtil.trimEnd(VfsUtilCore.getRelativeLocation(classFile, classRootForFile), ".class") + ".java/";
            return JarFileSystem.getInstance().findFileByPathIfCached(jarPath + entryFilePath);
        } catch (Exception e) {
            return null;
        }
    }

    private interface LineMappingProvider {

        DecompiledResult getDecompiled(VirtualFile classFile);
    }

    private static class BinaryFileDecompilerProvider implements LineMappingProvider {
        private static final LineMappingProvider INSTANCE = new BinaryFileDecompilerProvider();

        @Override
        public DecompiledResult getDecompiled(VirtualFile classFile) {
            PropertiesComponent.getInstance().setValue("decompiler.legal.notice.accepted", true);
            CharSequence content = LoadTextUtil.loadText(classFile);
            return new DecompiledResult(content, classFile.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY));
        }
    }

    private static class IntellijDecompilerProvider implements LineMappingProvider {
        private static final LineMappingProvider INSTANCE = new IntellijDecompilerProvider();
        private static final ThreadLocal<LineMappingResultSaver> RESULT_SAVER = new ThreadLocal<>();
        private static final Decompiler DECOMPILER = new IntellijDecompiler() {
            @Override
            protected IResultSaverEx getResultSaver(FileInfo file) {
                return RESULT_SAVER.get();
            }
        };

        @Override
        @SneakyThrows
        public DecompiledResult getDecompiled(VirtualFile classFile) {
            try (LineMappingResultSaver resultSaver = new LineMappingResultSaver()) {
                RESULT_SAVER.set(resultSaver);
                DECOMPILER.decompile(classFile);
                IResultSaverEx.Result result = resultSaver.getResult();
                if (result != null) {
                    return new DecompiledResult(result.content(),
                            new LineNumbersMapping.ArrayBasedMapping(result.mapping()));
                }
            } finally {
                RESULT_SAVER.remove();
            }
            return DecompiledResult.EXCEPTION;
        }
    }
}
