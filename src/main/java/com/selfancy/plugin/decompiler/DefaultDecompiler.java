package com.selfancy.plugin.decompiler;

import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.selfancy.plugin.Decompiler;
import com.selfancy.plugin.bundle.CodeSpyBundle;
import com.selfancy.plugin.decompiler.intellij.IntellijDecompiler;
import com.selfancy.plugin.utils.NotificationUtils;
import org.jetbrains.annotations.NotNull;

import static com.selfancy.plugin.bundle.CodeSpyBundle.message;

/**
 * {@link DefaultDecompiler}
 * <p>
 * Created by mike on 2024/03/31
 *
 * @see Decompiler
 */
public class DefaultDecompiler implements Decompiler {
    private final InternalDecompiler delegate;

    public static Decompiler create() {
        return new DecompilerImpl(new DefaultDecompiler());
    }

    public DefaultDecompiler() {
        this.delegate = new IntellijDecompiler();
    }

    @Override
    public void decompile(VirtualFile file) {
        delegate.decompile(file);
    }

    private record DecompilerImpl(Decompiler target) implements Decompiler {

        @Override
        public void decompile(VirtualFile file) {
            Project project = ProjectManager.getInstance().getDefaultProject();
            String title = CodeSpyBundle.message("decompiler.progress.decompile.title", file.getName());
            ProgressManager.getInstance().run(new Task.Backgroundable(project, title) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    Throwable ex = null;
                    try {
                        target.decompile(file);
                    } catch (ProcessCanceledException e) {
                        ProgressManager.canceled(indicator);
                        return;
                    } catch (Throwable e) {
                        ex = e;
                    }
                    if (ex != null) {
                        NotificationUtils.sendError(message("decompiler.notification.decompile.error",
                                ex.getMessage()));
                    } else {
                        BinaryFileTypeDecompilers.getInstance().notifyDecompilerSetChange();
                        NotificationUtils.sendInfo(message("decompiler.notification.decompile.success",
                                file.getName()));
                    }
                }
            });
        }
    }
}
