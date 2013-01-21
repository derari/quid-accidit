package de.hpi.accidit.trace;

import de.hpi.accidit.model.*;
import de.hpi.accidit.out.Out;

/**
 * A method call. 
 * 
 * Argument values are traced as {@link VariableTrace}s.
 * 
 * @author Arian Treffer
 */
public class CallTrace {
    
    private final CallTrace caller;
    private final ThreadTrace trace;
    private final Out out;
    private final MethodDescriptor method;
    private final Step stepCounter;
    private final boolean ignore;
    private final ObjectTrace instance;
    
    private ExitTrace exit;
    
    private final long step;
    private final int line;
    private final int depth;
    
    private int lastLine = -1;
    private int nextCall = -1;
    
    private int lastLogLine = -2;
    
    private boolean traced = false;
    
    public CallTrace(CallTrace caller, ThreadTrace trace, MethodDescriptor method, ObjectTrace instance, Step step, int callLine, int depth, boolean ignore) {
        this.caller = caller;
        this.trace = trace;
        this.out = trace.getModel().out;
        this.method = method;
        this.stepCounter = step;
        this.instance = instance;
        
        this.step = step.next();
        this.line = callLine;
        this.depth = depth;
        this.ignore = ignore;
    }

    public ThreadTrace getTrace() {
        return trace;
    }

    public long getStep() {
        return step;
    }

    public int getLine() {
        return line;
    }

    public int getDepth() {
        return depth;
    }

    public MethodDescriptor getMethod() {
        return method;
    }

    public ObjectTrace getInstance() {
        return instance;
    }
    
    public void line(int line) {
        lastLine = line;
    }

    public void call(int methodCode) {
        nextCall = methodCode;
    }

    public void ensurePersisted() {
        if (traced) return;
        traced = true;
        if (caller != null) caller.ensurePersisted();
        method.ensurePersisted();
        if (instance != null) instance.ensurePersisted();
        out.traceCall(this);
    }
    
    public boolean isTraced() {
        return traced;
    }

    public CallTrace enter(MethodDescriptor method, Object instance) {
//        if (method.getName().equals("parseURL")) {
//            System.out.print("");
//        }
        int nextDepth = depth+1;
        ObjectTrace otInstance = trace.getObjectTrace(instance);
        if (ignore || !method.implementsMethod(nextCall)) {
            nextDepth = trace.getCurrentDepth(2);
            if (nextDepth <= depth) {
                if (!trace.sanatizeStack()) return null;
            }
            if (!traceEvenThoughIgnoring(instance, otInstance)) {
                return new CallTrace(this, trace, method, otInstance, stepCounter, lastLine, nextDepth, true);
            }
        }
        CallTrace inv = new CallTrace(this, trace, method, otInstance, stepCounter, lastLine, nextDepth, false);
        inv.ensurePersisted();
        return inv;
    }
    
    /**
     * Return true iff the event related to the given object should be traced,
     * even though this call was flagged as ignored.
     * Should not be used for modification events.
     */
    private boolean traceEvenThoughIgnoring(Object obj, ObjectTrace ot) {
        return ObjectTrace.isPersisted(ot) &&
                (obj == null || isInteresting(obj));
    }
    
    /**
     * If we are in an ignored section and a java.lang.* Object occurs 
     * that was traced at some point, we probably still don't care.
     */
    private static boolean isInteresting(Object instance) {
        Class clazz = instance.getClass();
        if (clazz.isArray()) return false;
        String clazzName = clazz.getCanonicalName();
        return clazzName != null && !clazzName.startsWith("java.lang");
    }

    public void cancel(ObjectTrace throwable) {
        if (exit == null) failed(throwable);
        if (caller != null) {
            caller.cancel(throwable);
        }
    }

    public boolean returned(int methodCode, int line, Object result) {
        if (methodCode >= 0 && methodCode != method.getCodeId()) return false;
        if (ignore && !traced) return true;
        assertIsOnTopOfStack();
        try {
            PrimitiveType pt = method.getResultType();
            long valueId = trace.getValueId(pt, result);
            exit = new ExitTrace(step, true, line, stepCounter.next(), pt, valueId);
            out.traceExit(this, exit);
        } catch (RuntimeException re) {
            throw new RuntimeException(toString(), re);
        }
        return true;
    }
    
    public void failed(ObjectTrace throwable) {
        if (ignore && !traced) return;
        throwable.ensurePersisted();
        PrimitiveType pt = PrimitiveType.OBJECT;
        exit = new ExitTrace(step, false, lastLine, stepCounter.next(), pt, throwable.getId());
        out.traceExit(this, exit);
    }
    
    public void thrown(int line, Object t) {
        lastLine = line;
        ObjectTrace ot = trace.getObjectTrace(t);
        thrown(line, ot);
    }
    
    private void thrown(int line, ObjectTrace ot) {
        ThrowableTrace exTrace = new ThrowableTrace(line, stepCounter.next(), ot);
        if (ignore) {
            ot.setThrowEvent(this, exTrace);
        } else {
            ot.ensurePersisted();
            out.traceThrow(this, exTrace);
            ot.markThrowTraced();
        }
    }
    
    /**
     * Ensures the exception was traced as thrown. 
     * If this call is not ignored, the throw event will be persisted.
     */
    public void ensureThrown(ObjectTrace ot) {
        if (ot.isThrowTraced()) return;
        CallTrace ct = ot.getThrowCall();
        if (ct == null) {
            thrown(lastLine, ot);
        } else if (!ignore) {
            ct.ensurePersisted();
            out.traceThrow(ct, ot.getThrowEvent());
            ot.markThrowTraced();
        }
    }

    public void caught(int line, ObjectTrace ot) {
        assertIsOnTopOfStack();
        if (ignore) {
            if (!ot.isThrowTraced()) return;
        }
        lastLine = line;
        ThrowableTrace exTrace = new ThrowableTrace(line, stepCounter.next(), ot);
        out.traceCatch(this, exTrace);    
        ot.markCatchTraced();
    }

    public void argument(int argId, Object value) {
        if (ignore) return;
        variable(lastLine, argId, value, step);
    }
    
    public void variable(int line, int varId, Object value) {
        lastLine = line;
        if (ignore) return;
        variable(line, varId, value, stepCounter.next());
    }
    
    public void variable(int line, int varId, Object value, long step) {
        VarDescriptor var = method.getVariable(varId);
        if (var == null) return; // variables not traced
        try {
            PrimitiveType primType = var.getPrimitiveType();
            long valueId = trace.getValueId(primType, value);
            VariableTrace varTrace = new VariableTrace(var, step, line, primType, valueId);
            trace.getModel().out.traceVariable(this, varTrace);
        } catch (RuntimeException e) {
            throw new RuntimeException(var.toString(), e);
        }
    }

    public void put(int line, Object instance, int fieldId, Object value) {
        lastLine = line;
        ObjectTrace ot = trace.getObjectTrace(instance);
        if (ignore) {
            if (!ObjectTrace.isPersisted(ot)) return;
            ensurePersisted();
        }
        FieldTrace fieldTrace = fieldTrace(fieldId, value, ot, line);
        trace.getModel().out.tracePut(this, fieldTrace);
    }

    public void get(int line, Object instance, int fieldId, Object value) {
        lastLine = line;
        ObjectTrace ot = trace.getObjectTrace(instance);
        if (ignore) {
            if (!traceEvenThoughIgnoring(instance, ot)) return;
            ensurePersisted();
        }
        FieldTrace fieldTrace = fieldTrace(fieldId, value, ot, line);
        trace.getModel().out.traceGet(this, fieldTrace);
    }

    private FieldTrace fieldTrace(int fieldId, Object value, ObjectTrace otInstance, int line) {
        FieldDescriptor field = trace.getModel().getField(fieldId);
        try {
            if (otInstance != null) otInstance.ensurePersisted();
            PrimitiveType primType = field.getType().getPrimitiveType();
            long valueId = trace.getValueId(primType, value);
            return new FieldTrace(otInstance, field, line, stepCounter.next(), primType, valueId);
        } catch (RuntimeException e) {
            throw new RuntimeException(field.toString(), e);
        }
    }

    public void arrayStore(int line, Object instance, int index, Object value) {
        lastLine = line;
        ObjectTrace ot = trace.getObjectTrace(instance);
        if (ignore) {
            if (!ot.isPersisted()) return;
            ensurePersisted();
        }
        ArrayItemTrace arrayTrace = arrayTrace(index, value, ot, line);
        trace.getModel().out.traceArrayPut(this, arrayTrace);
    }

    public void arrayLoad(int line, Object instance, int index, Object value) {
        lastLine = line;
        ObjectTrace ot = trace.getObjectTrace(instance);
        if (ignore) {
            if (!traceEvenThoughIgnoring(instance, ot)) return;
            ensurePersisted();
        }
        ArrayItemTrace arrayTrace = arrayTrace(index, value, ot, line);
        trace.getModel().out.traceArrayGet(this, arrayTrace);
    }

    private ArrayItemTrace arrayTrace(int index, Object value, ObjectTrace otInstance, int line) {
        otInstance.ensurePersisted();
        TypeDescriptor valueType = otInstance.getType().getComponentType();
        PrimitiveType primType = valueType.getPrimitiveType();
        long valueId = trace.getValueId(primType, value);
        return new ArrayItemTrace(otInstance, index, line, stepCounter.next(), primType, valueId);
    }

    @Override
    public String toString() {
        return method.toString();
    }

    private void assertIsOnTopOfStack() {
//        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
//        for (int i = 1; i < stack.length; i++) {
//            StackTraceElement frame = stack[i];
//            if (!frame.getClassName().startsWith("de.hpi") ||
//                    frame.getClassName().startsWith("de.hpi.accidit.testapp")) {
//                String mName = frame.getMethodName();
//                if (!mName.equals(method.getName())) {
//                    throw new IllegalStateException("Expected " + method + ",\n got " + frame);
//                }
//                return;
//            }
//        }
    }

    boolean log() {
        if (lastLogLine == lastLine) {
            System.out.print('/');
            return false;
        }
        lastLogLine = lastLine;
        System.out.print(method.getName());
        System.out.print(':');
        System.out.print(lastLine);
        System.out.print(' ');
        return true;
    }

}
