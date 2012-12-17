package de.hpi.accidit.trace;

/**
 * Traces a throwable that was thrown or caught.
 */
public class ThrowableTrace {
    
    private final int line;
    private final long step;
    private final ObjectTrace exception;

    public ThrowableTrace(int line, long step, ObjectTrace exception) {
        this.line = line;
        this.step = step;
        this.exception = exception;
    }

    public int getLine() {
        return line;
    }

    public long getStep() {
        return step;
    }

    public ObjectTrace getThrowable() {
        return exception;
    }

    @Override
    public String toString() {
        return exception.toString();
    }
    
}
