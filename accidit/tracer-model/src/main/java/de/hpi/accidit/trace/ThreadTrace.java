package de.hpi.accidit.trace;

import de.hpi.accidit.model.*;
import java.util.Stack;

/**
 *
 * @author Arian Treffer
 */
public class ThreadTrace {

    private final Model model;
    private final int id;
    private final Step step = new Step();
    private final EndCallback endCallback;
    
    private String name = null;
    private int baseStack = -1;
    
    private CallTrace root = null;
    private CallTrace invocation = null;
    private Stack<CallTrace> stack = new Stack<>();
    
    private final WeakIdentityMap<Object, ObjectTrace> objects = new WeakIdentityMap<>();
    private long nextObjectId = 1; // 0 reserved for `null`
    
    private boolean cflow = false;

    public ThreadTrace(Model model, int id, EndCallback endCallback) {
        this.model = model;
        this.id = id;
        this.endCallback = endCallback;
    }

    public int getId() {
        return id;
    }

    Model getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public CallTrace getRoot() {
        return root;
    }
    
    public synchronized ObjectTrace getObjectTrace(Object object) {
        if (object == null) return null;
        ObjectTrace ot = objects.get(object);
        if (ot == null) {
            ot = new ObjectTrace(this, object, nextObjectId());
            objects.put(object, ot);
            ot.getType().ensurePersisted();
            model.out.traceObject(this, ot);
        }
        return ot;
    }
    
    public long getValueId(PrimitiveType primType, Object value) {
        if (primType == PrimitiveType.OBJECT) {
            return getObjectId(value);
        } else {
            return primType.toValueId(value);
        }
    }
    
    public long getObjectId(Object object) {
        return object == null ? 0 : getObjectTrace(object).getId();
    }

    public void end() {
        if (cflow) return;
        cflow = true;
        if (getStackSize(1) <= baseStack) {
            endTrace();
        }
        cflow = false;
    }
    
    private void endTrace() {
        model.out.end(this);
        endCallback.ended(this);
    }
    
    /** line number before method invocation */
    public void line(int line) {
        invocation.line(line);
    }
    
    public void enter(int methodCode) {
        if (cflow) return;
        cflow = true;
        MethodDescriptor method = model.getMethod(methodCode);
        if (invocation == null) {
            invocation = new CallTrace(this, method, step, -1, 0);
            assert root == null : "Should not have traced yet";
            root = invocation;
            name = root.getMethod().getName();
            baseStack = getStackSize(1);
            model.out.begin(this);
            model.out.traceCall(root);
        } else {
            invocation = invocation.enter(method);
        }
        stack.push(invocation);
        cflow = false;
    }
    
    public void returned(int line, Object result) {
        if (cflow) return;
        cflow = true;
        stack.pop().returned(line, result);
        invocation = stack.isEmpty() ? null : stack.peek();
        cflow = false;
    }
    
    public void thrown(int line, Object t) {
        if (cflow) return;
        cflow = true;
        invocation.thrown(line, t);
        cflow = false;
    }
    
    public void caught(int line, Object t) {
        if (cflow) return;
        cflow = true;
        ObjectTrace exTrace = getObjectTrace(t);
        int depth = getStackSize(1) - baseStack;
        while (invocation.getDepth() > depth) {
            stack.pop().failed(exTrace);
            if (stack.isEmpty()) {
                endTrace();
                return;
            }
            invocation = stack.peek();
        }
        invocation.caught(line, t);
        cflow = false;
    }
    
    public void argument(int argId, Object value) {
        if (cflow) return;
        cflow = true;
        invocation.argument(argId, value);
        cflow = false;
    }
    
    public void variable(int line, int varId, Object value) {
        if (cflow) return;
        cflow = true;
        invocation.variable(line, varId, value);
        cflow = false;
    }

    public void put(Object instance, Object value, int fieldId, int line) {
        if (cflow) return;
        cflow = true;
        invocation.put(line, instance, fieldId, value);
        cflow = false;
    }
    
    public void get(Object instance, Object value, int fieldId, int line) {
        if (cflow) return;
        cflow = true;
        invocation.get(line, instance, fieldId, value);
        cflow = false;
    }
    
    public void aryPut(Object instance, Object value, int index, int line) {
        if (cflow) return;
        cflow = true;
        invocation.aryPut(line, instance, index, value);
        cflow = false;
    }
    
    public void aryGet(Object instance, Object value, int index, int line) {
        if (cflow) return;
        cflow = true;
        invocation.aryGet(line, instance, index, value);
        cflow = false;
    }
    
    protected int getStackSize(int i) {
        return Thread.currentThread().getStackTrace().length - i;
    }

    long nextObjectId() {
        return nextObjectId++;
    }

    public static interface EndCallback {
        
        void ended(ThreadTrace trace);
        
    }
    
}
