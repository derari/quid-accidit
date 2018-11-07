package de.hpi.accidit.trace;

import de.hpi.accidit.model.FieldDescriptor;
import de.hpi.accidit.model.Model;
import de.hpi.accidit.model.PrimitiveType;
import de.hpi.accidit.model.TypeDescriptor;
import de.hpi.accidit.out.Out;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
    private Object existingObject;

    public ObjectTrace(ThreadTrace trace, Object object, boolean newObject) {
        this(trace, typeOf(trace.getModel(), object), arrayLen(object), newObject ? null : object);
    }
    
    public ObjectTrace(ThreadTrace trace, TypeDescriptor type, int arrayLength, Object existingObject) {
        this.trace = trace;
        this.type = type;
        this.arrayLength = arrayLength;
        this.existingObject = existingObject;
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
        ensurePersisted(0);
    }
    
    private void ensurePersisted(int depth) {
        if (persisted) return;
        synchronized (trace.getModel()) {
            if (persisted) return;
            persisted = true;
            type.ensurePersisted();
            if (type.getName().equals("java.lang.String")) depth = 0;
            this.id = trace.nextObjectId();
            trace.getModel().out.traceObject(trace, this);
            if (existingObject != null && depth < 5) {
                traceFields(depth);
            }
        }
    }
    
    private void traceFields(int depth) {
        if (arrayLength > -1) {
            traceItems(depth);
            return;
        }
        int traceId = trace.getId();
        Out out = trace.getModel().out;
        Object self = existingObject;
        existingObject = null;
        for (Class c = self.getClass(); c != null; c = c.getSuperclass()) {
            TypeDescriptor td = trace.getModel().getType(c.getName(), c.getClassLoader());
            for (Field f: c.getDeclaredFields()) {
                if ((f.getModifiers() & Modifier.STATIC) != 0) continue;
                Object value;
                try {
                    f.setAccessible(true);
                    value = f.get(self);
                } catch (IllegalAccessException e) {
                    continue;
                }
                FieldDescriptor fd = td.getField(f.getName(), descriptor(f.getType()));
                PrimitiveType primType = fd.getType().getPrimitiveType();
                long valueId;
                if (value == null) {
                    valueId = 0;
                } else if (primType == PrimitiveType.OBJECT) {
                    ObjectTrace ot = trace.getObjectTrace(value);
                    ot.ensurePersisted(depth+1);
                    valueId = ot.getId();
                } else {
                    valueId = primType.toValueId(value);
                }
                FieldTrace ft = new FieldTrace(this, fd, -1, 0, primType, valueId);
                out.traceExistingField(traceId, ft);
            }
        }
    }
    
    private void traceItems(int depth) {
        int traceId = trace.getId();
        Out out = trace.getModel().out;
        Object self = existingObject;
        existingObject = null;
        TypeDescriptor cd = getType().getComponentType();
        PrimitiveType primType = cd.getPrimitiveType();
        for (int i = 0; i < arrayLength; i++) {
            Object value = Array.get(self, i);
            long valueId;
            if (value == null) {
                valueId = 0;
            } else if (primType == PrimitiveType.OBJECT) {
                ObjectTrace ot = trace.getObjectTrace(value);
                ot.ensurePersisted(depth+1);
                valueId = ot.getId();
            } else {
                valueId = primType.toValueId(value);
            }
            ArrayItemTrace it = new ArrayItemTrace(this, i, -1, 0, primType, valueId);
            out.traceExistingItem(traceId, it);
        }
    }
    
    private String descriptor(Class c) {
        if (c.isArray()) return c.getName();
        String s = Array.newInstance(c, 0).getClass().getName();
        return s.substring(1);
    }

    public boolean isPersisted() {
        return persisted;
    }
    
}
