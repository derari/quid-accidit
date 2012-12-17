package de.hpi.accidit.trace;

import de.hpi.accidit.model.Model;
import de.hpi.accidit.model.TypeDescriptor;
import java.lang.reflect.Array;

/**
 *
 * @author Arian Treffer
 */
public class ObjectTrace {
    
    private static TypeDescriptor typeOf(Model model, Object object) {
        String clazz = TypeDescriptor.className(object.getClass());
        return model.getType(clazz, object.getClass().getClassLoader());
    }
    
    private static int arrayLen(Object object) {
        if (object.getClass().isArray()) {
            return Array.getLength(object);
        } else {
            return -1;
        }
    }
    
    public static boolean isPersisted(ObjectTrace ot) {
        return ot != null && ot.isPersisted();
    }
    
    private final ThreadTrace trace;
    private final TypeDescriptor type;
    private long id = -1;
    private final int arrayLength;
    
    private boolean throwTraced = false;
    private CallTrace throwCall = null;
    private ThrowableTrace throwEvent = null;
    private boolean persisted = false;

    public ObjectTrace(ThreadTrace trace, Object object) {
        this(trace, typeOf(trace.getModel(), object), arrayLen(object));
    }
    
    public ObjectTrace(ThreadTrace trace, TypeDescriptor type, int arrayLength) {
        this.trace = trace;
        this.type = type;
        this.arrayLength = arrayLength;
    }

    public long getId() {
        if (id < 0) throw new IllegalStateException("not persisted");
        return id;
    }

    public TypeDescriptor getType() {
        return type;
    }

    public int getArrayLength() {
        return arrayLength;
    }

    @Override
    public String toString() {
        return String.format("(%s_%05d", type, id);
    }    

    public boolean isThrowTraced() {
        return throwTraced;
    }

    public ThrowableTrace getThrowEvent() {
        return throwEvent;
    }

    public CallTrace getThrowCall() {
        return throwCall;
    }

    public void markThrowTraced() {
        this.throwCall = null;
        this.throwEvent = null;
        this.throwTraced = true;
    }
    
    public void markCatchTraced() {
        this.throwTraced = false;
    }

    public void setThrowEvent(CallTrace throwCall, ThrowableTrace throwEvent) {
        this.throwCall = throwCall;
        this.throwEvent = throwEvent;
        this.throwTraced = false;
    }
    
    public void ensurePersisted() {
        if (persisted) return;
        synchronized (trace.getModel()) {
            if (persisted) return;
            persisted = true;
            type.ensurePersisted();
            this.id = trace.nextObjectId();
            trace.getModel().out.traceObject(trace, this);
        }
    }

    public boolean isPersisted() {
        return persisted;
    }
    
}
