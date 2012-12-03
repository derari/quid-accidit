package de.hpi.accidit.trace;

import de.hpi.accidit.model.Model;
import de.hpi.accidit.model.TypeDescriptor;

/**
 *
 * @author Arian Treffer
 */
public class ObjectTrace {
    
    private static TypeDescriptor typeOf(Model model, Object object) {
        return model.getType(object.getClass().getCanonicalName());
    }
    
    private final TypeDescriptor type;
    private final long id;

    public ObjectTrace(ThreadTrace trace, Object object, long id) {
        this(typeOf(trace.getModel(), object), id);
    }
    
    public ObjectTrace(TypeDescriptor type, long id) {
        this.type = type;
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public TypeDescriptor getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("(%s_%05d", type, id);
    }    
    
}
