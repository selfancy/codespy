package com.selfancy.plugin.decompiler;

import com.intellij.execution.filters.LineNumbersMapping;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import lombok.Getter;
import lombok.Setter;

/**
 * {@link DecompiledResult}
 * <p>
 * Created by mike on 2024/05/18
 */
@Getter
public class DecompiledResult {
    public static final Key<Boolean> IS_MY_DECOMPILED = Key.create("IS_MY_DECOMPILED");
    public static DecompiledResult INVALID = new DecompiledResult((CharSequence) null, null);
    public static DecompiledResult EXCEPTION = new DecompiledResult(null, null, null, Boolean.TRUE);
    @Setter
    private VirtualFile javaFile;
    @Setter
    private VirtualFile classFile;
    private CharSequence content;
    private LineNumbersMapping lineMapping;
    private Boolean alreadyDecompiled = null;
    private Boolean exception = null;

    public DecompiledResult(CharSequence content, LineNumbersMapping lineMapping) {
        this.content = content;
        this.lineMapping = lineMapping;
    }

    public DecompiledResult(VirtualFile javaFile, VirtualFile classFile) {
        this.javaFile = javaFile;
        this.classFile = classFile;
        this.lineMapping = classFile.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY);
        if (this.lineMapping == null) {
            this.lineMapping = javaFile.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY);
        }
        if (this.lineMapping != null) {
            this.alreadyDecompiled = Boolean.TRUE;
        }
    }

    public DecompiledResult(CharSequence content, LineNumbersMapping lineMapping, Boolean alreadyDecompiled, Boolean exception) {
        this.content = content;
        this.lineMapping = lineMapping;
        this.alreadyDecompiled = alreadyDecompiled;
        this.exception = exception;
    }

    public boolean hasMapping() {
        return lineMapping != null;
    }

    public PsiFile getJavaPsiFile(Project project) {
        if (javaFile != null) {
            return PsiManager.getInstance(project).findFile(javaFile);
        }
        return null;
    }

    public PsiFile getClassPsiFile(Project project) {
        if (classFile != null) {
            return PsiManager.getInstance(project).findFile(classFile);
        }
        return null;
    }

    public void saveMappingIfDecompiled() {
        if (hasMapping()) {
            if (classFile != null) {
                classFile.putUserData(IS_MY_DECOMPILED, Boolean.TRUE);
                classFile.putUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY, lineMapping);
            }
            if (javaFile != null) {
                javaFile.putUserData(IS_MY_DECOMPILED, Boolean.TRUE);
                javaFile.putUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY, lineMapping);
            }
        }
    }
}
