package com.selfancy.plugin.action;

import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.ide.projectView.impl.nodes.NamedLibraryElementNode;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.selfancy.plugin.Decompiler;
import com.selfancy.plugin.decompiler.DefaultDecompiler;
import com.selfancy.plugin.utils.FileUtils;
import com.selfancy.plugin.utils.JarUtils;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 反编译按钮事件处理
 * <p>
 * Created by mike on 2024/03/29
 */
final class DecompilerAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Navigatable[] selectItems = e.getData(CommonDataKeys.NAVIGATABLE_ARRAY);
        if (selectItems != null) {
            List<SelectedFile> files = readSelectedFiles(selectItems);
            e.getPresentation().setEnabledAndVisible(!files.isEmpty());
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Navigatable[] selectItems = e.getData(CommonDataKeys.NAVIGATABLE_ARRAY);
        if (selectItems != null) {
            List<SelectedFile> files = readSelectedFiles(selectItems);
            if (!files.isEmpty()) {
                Decompiler decompiler = DefaultDecompiler.create();
                files.forEach(file -> {
                    try {
                        decompiler.decompile(file.jarFile());
                    } finally {
                        attachSourceJar(e.getProject(), file);
                    }
                });
            }
        }
    }

    @SneakyThrows
    private List<SelectedFile> readSelectedFiles(Navigatable[] selectItems) {
        List<SelectedFile> files = new ArrayList<>();
        for (Navigatable navigatable : selectItems) {
            if (navigatable instanceof PsiFileNode psiFileNode) {
                files.add(new SelectedFile(psiFileNode.getVirtualFile(), null));
            }
            if (navigatable instanceof PsiDirectoryNode psiDirectoryNode) {
                files.add(new SelectedFile(psiDirectoryNode.getVirtualFile(), null));
            }
            if (navigatable instanceof NamedLibraryElementNode elementNode) {
                String libraryName = elementNode.getValue().getName();
                VirtualFile[] rootFiles = elementNode.getValue().getOrderEntry().getRootFiles(OrderRootType.CLASSES);
                files.addAll(Arrays.stream(rootFiles).map(file -> new SelectedFile(file, libraryName)).toList());
            }
        }
        return files.stream()
                .filter(it -> ArchiveFileType.INSTANCE == it.jarFile().getFileType())
                .filter(it -> FileUtils.JAR_FILE_EXTENSION.equals(it.jarFile().getExtension()))
                .collect(Collectors.toList());
    }

    private record SelectedFile(VirtualFile jarFile, String libraryName) {
        private boolean isInModule() {
            return libraryName != null;
        }
    }

    private void attachSourceJar(Project project, SelectedFile selectedFile) {
        if (project == null || !selectedFile.isInModule()) return;
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
        Library library = libraryTable.getLibraryByName(selectedFile.libraryName());
        if (library != null) {
            JarUtils.attachSourceJar(library);
        }
    }
}
