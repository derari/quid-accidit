package de.hpi.accidit.jditracer.model;

/**
 *
 * @author Arian Treffer
 */
public class ThrowTrace extends LocationTrace {
    
    private final int step;
    private final ObjectTrace exception;

    public ThrowTrace(int step, ObjectTrace exception) {
        this.step = step;
        this.exception = exception;
    }

    public int getStep() {
        return step;
    }

    public ObjectTrace getException() {
        return exception;
    }
    
}
