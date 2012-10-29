package de.hpi.accidit.jditracer.model;

/**
 *
 * @author Arian Treffer
 */
public class FieldAccessTrace extends LocationTrace {

    private final long step;
    private final ObjectTrace object;
    private final FieldDescriptor field;

    public FieldAccessTrace(long step, ObjectTrace o, FieldDescriptor field) {
        this.step = step;
        this.field = field;
        this.object = o;
        if (o != null) o.ensureIsTraced();
    }

    public long getStep() {
        return step;
    }

    public FieldDescriptor getField() {
        return field;
    }

    public Long getObjectId() {
        return object != null ? object.getId() : null;
    }
    
}
