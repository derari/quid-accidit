package de.hpi.accidit.jditracer.model;

/**
 *
 * @author Arian Treffer
 */
public class CatchTrace extends LocationTrace {
    
    private final int step;
    private final ObjectTrace exception;

    public CatchTrace(int step, ObjectTrace exception) {
        this.step = step;
        this.exception = exception;
        exception.ensureIsTraced();
    }

    public int getStep() {
        return step;
    }

    public ObjectTrace getException() {
        return exception;
    }
    
}
