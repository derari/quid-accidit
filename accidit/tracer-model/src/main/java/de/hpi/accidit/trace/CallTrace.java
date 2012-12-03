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
    
    private final ThreadTrace trace;
    private final Out out;
    private final MethodDescriptor method;
    private final Step stepCounter;
    
    private final long step;
    private final int line;
    private final int depth;
    
    private int lastLine;
    
    public CallTrace(ThreadTrace trace, MethodDescriptor method, Step step, int callLine, int depth) {
        this.trace = trace;
        this.out = trace.getModel().out;
        this.method = method;
        this.stepCounter = step;
        method.ensurePersisted();
        
        this.step = step.next();
        this.line = callLine;
        this.depth = depth;
        lastLine = line;
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
    
    public void line(int line) {
        lastLine = line;
    }

    public CallTrace enter(MethodDescriptor method) {
        CallTrace inv = new CallTrace(trace, method, stepCounter, lastLine, depth+1);
        out.traceCall(inv);
        return inv;
    }

    public void returned(int line, Object result) {
        PrimitiveType pt = method.getResultType();
        long valueId = trace.getValueId(pt, result);
        ExitTrace exit = new ExitTrace(step, true, line, stepCounter.next(), pt, valueId);
        out.traceExit(this, exit);
    }
    
    public void failed(ObjectTrace throwable) {
        PrimitiveType pt = PrimitiveType.OBJECT;
        ExitTrace exit = new ExitTrace(step, false, lastLine, stepCounter.next(), pt, throwable.getId());
        out.traceExit(this, exit);
    }
    
    public void thrown(int line, Object t) {
        lastLine = line;
        ObjectTrace ot = trace.getObjectTrace(t);
        ThrowableTrace exTrace = new ThrowableTrace(line, stepCounter.next(), ot);
        out.traceThrow(this, exTrace);
    }

    public void caught(int line, Object t) {
        ObjectTrace ot = trace.getObjectTrace(t);
        ThrowableTrace exTrace = new ThrowableTrace(line, stepCounter.next(), ot);
        out.traceCatch(this, exTrace);
    }

    public void argument(int argId, Object value) {
        variable(lastLine, argId, value, step);
    }
    
    public void variable(int line, int varId, Object value) {
        lastLine = line;
        variable(line, varId, value, stepCounter.next());
    }
    
    public void variable(int line, int varId, Object value, long step) {
        VarDescriptor var = method.getVariable(varId);
        PrimitiveType primType = var.getPrimitiveType();
        long valueId = trace.getValueId(primType, value);
        VariableTrace varTrace = new VariableTrace(var, step, line, primType, valueId);
        trace.getModel().out.traceVariable(this, varTrace);
    }

    public void put(int line, Object instance, int fieldId, Object value) {
        lastLine = line;
        FieldTrace fieldTrace = fieldTrace(fieldId, value, instance, line);
        trace.getModel().out.tracePut(this, fieldTrace);
    }

    public void get(int line, Object instance, int fieldId, Object value) {
        lastLine = line;
        FieldTrace fieldTrace = fieldTrace(fieldId, value, instance, line);
        trace.getModel().out.traceGet(this, fieldTrace);
    }

    private FieldTrace fieldTrace(int fieldId, Object value, Object instance, int line) {
        FieldDescriptor field = trace.getModel().getField(fieldId);
        PrimitiveType primType = field.getType().getPrimitiveType();
        long valueId = trace.getValueId(primType, value);
        ObjectTrace ot = trace.getObjectTrace(instance);
        return new FieldTrace(ot, field, line, stepCounter.next(), primType, valueId);
    }

    public void aryPut(int line, Object instance, int index, Object value) {
        lastLine = line;
        ArrayItemTrace arrayTrace = arrayTrace(index, value, instance, line);
        trace.getModel().out.traceArrayPut(this, arrayTrace);
    }

    public void aryGet(int line, Object instance, int index, Object value) {
        lastLine = line;
        ArrayItemTrace arrayTrace = arrayTrace(index, value, instance, line);
        trace.getModel().out.traceArrayGet(this, arrayTrace);
    }

    private ArrayItemTrace arrayTrace(int index, Object value, Object instance, int line) {
        ObjectTrace ot = trace.getObjectTrace(instance);
        TypeDescriptor valueType = ot.getType().getComponentType();
        PrimitiveType primType = valueType.getPrimitiveType();
        long valueId = trace.getValueId(primType, value);
        return new ArrayItemTrace(ot, index, line, stepCounter.next(), primType, valueId);
    }

    @Override
    public String toString() {
        return method.toString();
    }

}
