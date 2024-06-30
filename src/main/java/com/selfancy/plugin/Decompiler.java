package com.selfancy.plugin;

import com.intellij.openapi.vfs.VirtualFile;

/**
 * Decompiler, default is write to file dir
 * <p>
 * Created by mike on 2024/03/30
 */
public interface Decompiler {

    /**
     * decompileJar
     */
    void decompile(VirtualFile file);
}
