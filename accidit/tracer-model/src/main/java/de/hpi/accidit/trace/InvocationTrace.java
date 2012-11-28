package de.hpi.accidit.trace;

import de.hpi.accidit.model.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Arian Treffer
 */
public class InvocationTrace {
    
    private final ThreadTrace trace;
    private final MethodDescriptor method;
    private final Step step;
    
    private final long entry;
    private final int callLine;
    private final int depth;
    private final List<InvocationTrace> invocations = new ArrayList<>();
    private final List<VariableTrace> variables = new ArrayList<>();
    
    private boolean returned = false;
    private long result = -1;

    public InvocationTrace(ThreadTrace trace, MethodDescriptor method, Step step, int callLine, int depth) {
        this.trace = trace;
        this.method = method;
        this.step = step;
        method.ensurePersisted();
        
        this.entry = step.next();
        this.callLine = callLine;
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public MethodDescriptor getMethod() {
        return method;
    }

    public List<InvocationTrace> getInvocations() {
        return invocations;
    }

    public boolean isReturned() {
        return returned;
    }

    public long getResult() {
        return result;
    }

    public InvocationTrace enter(int line, MethodDescriptor method) {
        InvocationTrace i = new InvocationTrace(trace, method, step, line, depth+1);
        invocations.add(i);
        return i;
    }

    public void returned(Object result) {
        PrimitiveType pt = method.getResultType();
        long valueId;
        if (pt == PrimitiveType.OBJECT) {
            valueId = trace.getObjectTrace(result).getId();
        } else {
            valueId = pt.toValueId(result);
        }
        returned = true;
        this.result = valueId;
    }

    public void argument(int argId, Object value) {
        VarDescriptor var = method.getVariable(argId);
    }
    
    @Override
    public String toString() {
        return String.format("%-3d:%4d(%s %s:%s_%d", callLine, entry, method, returned ? "R" : "E", method.getResultType().getKey(), result);
    }

}
