package de.hpi.accidit.jditracer.model;

/**
 *
 * @author Arian Treffer
 */
public class FieldTrace extends ValueTrace {

    private final ObjectTrace object;
    private final FieldDescriptor field;

    public FieldTrace(ObjectTrace o, FieldDescriptor field) {
        this.field = field;
        this.object = o;
        if (o != null) o.ensureIsTraced();
    }

    public FieldDescriptor getField() {
        return field;
    }

    public Long getObjectId() {
        return object != null ? object.getId() : null;
    }
    
}
