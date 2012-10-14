package de.hpi.accidit.jditracer.model;

/**
 *
 * @author ArianTreffer
 */
public abstract class ValueTrace extends LocationTrace {
    
    protected long step = -1;
    protected int primitiveId = 1;
    protected long value = 0;
    
    public long getStep() {
        return step;
    }

    public int getPrimitiveId() {
        return primitiveId;
    }

    public long getValue() {
        return value;
    }
    
}
