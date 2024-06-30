package com.selfancy.plugin.debug;

import com.intellij.debugger.SourcePosition;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * {@link SourcePositionWrapper}
 * <p>
 * Created by mike on 2024/05/18
 *
 * @see SourcePosition
 */
@RequiredArgsConstructor
abstract class SourcePositionWrapper extends SourcePosition {
    protected final SourcePosition delegate;

    @Override
    public @NotNull PsiFile getFile() {
        return delegate.getFile();
    }

    @Override
    public PsiElement getElementAt() {
        return delegate.getElementAt();
    }

    @Override
    public int getLine() {
        return delegate.getLine();
    }

    @Override
    public int getOffset() {
        return delegate.getOffset();
    }

    @Override
    public Editor openEditor(boolean b) {
        return delegate.openEditor(b);
    }

    @Override
    public void navigate(boolean b) {
        delegate.navigate(b);
    }

    @Override
    public boolean canNavigate() {
        return delegate.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return delegate.canNavigateToSource();
    }
}
