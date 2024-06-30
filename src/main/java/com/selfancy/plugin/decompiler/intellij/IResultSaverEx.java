package com.selfancy.plugin.decompiler.intellij;

import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.IOException;

/**
 * {@link IResultSaverEx}
 * <p>
 * Created by mike on 2024/04/01
 *
 * @see IResultSaver
 */
public interface IResultSaverEx extends IResultSaver, AutoCloseable {

    record Result(String content, boolean enableSourceMapping, int[] mapping) {
    }

    @Override
    default void close() throws IOException {
    }
}
