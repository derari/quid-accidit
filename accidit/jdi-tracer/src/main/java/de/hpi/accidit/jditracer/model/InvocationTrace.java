package de.hpi.accidit.jditracer.model;

import java.util.*;

/**
 *
 * @author Arian Treffer
 */
public class InvocationTrace extends ValueTrace {

    //private final long testId;
    private final MethodDescriptor method;
    private final long entry;
    private final int depth;
    private final int callLine;
    private long exit;
    private boolean ret;
    
    private ObjectTrace thisObject;
    
    private final List<LocalVarTrace> locals = new ArrayList<>();
    private final List<FieldTrace> modifications = new ArrayList<>();
    private final List<FieldAccessTrace> accesses = new ArrayList<>();
    private final List<ThrowTrace> exThrows = new ArrayList<>();
    private final List<CatchTrace> exCatches = new ArrayList<>();
    
    public InvocationTrace(long testId, MethodDescriptor method, long enter, int depth, int callLine, ObjectTrace thisObject) {
        //this.testId = testId;
        this.method = method;
        method.requireInitialized();
        this.entry = enter;
        this.depth = depth;
        this.callLine = callLine;
        this.thisObject = thisObject;
    }

    void exit(long exit, boolean ret) {
        this.exit = exit;
        this.ret = ret;
    }

    void setLocal(LocalVarTrace lvTrace) {
        locals.add(lvTrace);
    }
    
    void modification(FieldTrace fTrace) {
        modifications.add(fTrace);
    }

    void access(FieldAccessTrace fTrace) {
        accesses.add(fTrace);
    }

    void exCatch(CatchTrace cTrace) {
        exCatches.add(cTrace);
    }
    
    void exThrow(ThrowTrace tTrace) {
        exThrows.add(tTrace);
    }

    public long getEntry() {
        return entry;
    }

    public long getExit() {
        return exit;
    }
    
    public MethodDescriptor getMethod() {
        return method;
    }

    public boolean ignoreInternals() {
        return method.ignoreInternals();
    }
    
    public List<LocalVarTrace> getLocals() {
        return locals;
    }

    public List<FieldTrace> getModifications() {
        return modifications;
    }

    public List<FieldAccessTrace> getAccesses() {
        return accesses;
    }

    public List<ThrowTrace> getExThrows() {
        return exThrows;
    }

    public List<CatchTrace> getExCatches() {
        return exCatches;
    }

    public int getDepth() {
        return depth;
    }

    public int getCallLine() {
        return callLine;
    }

    public boolean isRet() {
        return ret;
    }

    public int getReturnLine() {
        return super.getLine();
    }

    @Override
    public int getLine() {
        throw new UnsupportedOperationException();
    }

    public Long getThisObjectId() {
        return thisObject != null ? thisObject.getId() : null;
    }

    @Override
    public String toString() {
        return depth + ") " + 
                (thisObject != null ? thisObject.toString() : "") +
                ">" + method.getName();
    }
    
    void dump() {
        System.out.printf("%8d-%8d %s %s %s\n", 
                entry, exit, method.getName(), method.getSignature(), ret ? "<" : "!");
    }

}
