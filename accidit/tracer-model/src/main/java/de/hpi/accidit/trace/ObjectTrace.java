package de.hpi.accidit.trace;

import de.hpi.accidit.model.Model;
import de.hpi.accidit.model.TypeDescriptor;

/**
 *
 * @author Arian Treffer
 */
public class ObjectTrace {
    
    public static ObjectTrace voidTrace(ThreadTrace trace) {
        return new ObjectTrace(trace, trace.getModel().getType("void"));
    }
    
    private static TypeDescriptor typeOf(Model model, Object object) {
        return model.getType(object.getClass().getCanonicalName());
    }
    
    private final ThreadTrace trace;
    private final TypeDescriptor type;
    private long id = -1;

    public ObjectTrace(ThreadTrace trace, Object object) {
        this(trace, typeOf(trace.getModel(), object));
    }
    
    public ObjectTrace(ThreadTrace trace, TypeDescriptor type) {
        this.trace = trace;
        this.type = type;
    }

    public long getId() {
        if (id < 0) ensurePersisted();
        return id;
    }

    private void ensurePersisted() {
        if (id >= 0) return;
        id = trace.nextObjectId();
        type.ensurePersisted();
        trace.getModel().out.traceObject(this);
    }

    @Override
    public String toString() {
        return String.format("(%s_%05d", type, id);
    }    
    
}
