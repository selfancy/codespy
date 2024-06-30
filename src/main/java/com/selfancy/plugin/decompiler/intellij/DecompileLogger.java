package com.selfancy.plugin.decompiler.intellij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.experimental.StandardException;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;

/**
 * {@link DecompileLogger}
 * <p>
 * Created by mike on 2024/05/18
 *
 * @see IFernflowerLogger
 */
class DecompileLogger extends IFernflowerLogger {
    private static final Logger LOGGER = Logger.getInstance(DecompileLogger.class);
    static final IFernflowerLogger INSTANCE = new DecompileLogger();

    @StandardException
    static class InternalException extends RuntimeException {
    }

    @Override
    public void writeMessage(String message, Severity severity) {
        switch (severity) {
            case ERROR, WARN -> LOGGER.warn(message);
            case INFO -> LOGGER.info(message);
            default -> LOGGER.debug(message);
        }
    }

    @Override
    public void writeMessage(String message, Severity severity, Throwable t) {
        if (t instanceof InternalException ex) throw ex;
        if (t instanceof ProcessCanceledException ex) throw ex;
        if (t instanceof InterruptedException ex) throw new ProcessCanceledException(ex);

        if (severity == Severity.ERROR) {
            throw new InternalException(message, t);
        } else {
            switch (severity) {
                case WARN -> LOGGER.warn(message, t);
                case INFO -> LOGGER.info(message, t);
                default -> LOGGER.debug(message, t);
            }
        }
    }
}
