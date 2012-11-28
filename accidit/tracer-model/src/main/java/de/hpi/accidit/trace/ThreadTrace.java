package de.hpi.accidit.trace;

import de.hpi.accidit.model.*;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Stack;

/**
 *
 * @author Arian Treffer
 */
public class ThreadTrace {

    private final Model model;
    private final Step step = new Step();
    
    private String name = null;
    private int baseStack = -1;
    
    private InvocationTrace root = null;
    private InvocationTrace invocation = null;
    private Stack<InvocationTrace> stack = new Stack<>();
    
    private final WeakIdentityMap<Object, ObjectTrace> objects = new WeakIdentityMap<>();
    private long nextObjectId = 1; // 0 reserved for `null`
    
    public ThreadTrace(Model model) {
        this.model = model;
        
    }

    Model getModel() {
        return model;
    }

    public void end() {
        model.out.traceContent(this);
    }

    public String getName() {
        return name;
    }

    public InvocationTrace getRoot() {
        return root;
    }
    
    public ObjectTrace getObjectTrace(Object object) {
        ObjectTrace ot = objects.get(object);
        if (ot == null) {
            ot = new ObjectTrace(this, object);
            objects.put(object, ot);
        }
        return ot;
    }
    
    public void enter(int line, int methodCode) {
        MethodDescriptor method = model.getMethod(methodCode);
        if (invocation == null) {
            invocation = new InvocationTrace(this, method, step, line, 0);
            assert root == null : "Should not have traced yet";
            root = invocation;
            name = root.getMethod().getName();
            baseStack = getStackSize(1);
            model.out.traceHead(this);
        } else {
            invocation = invocation.enter(line, method);
        }
        stack.push(invocation);
    }
    
    public void returned(Object result) {
        stack.pop().returned(result);
        invocation = stack.isEmpty() ? null : stack.peek();
    }
    
    public void argument(int argId, Object value) {
        invocation.argument(argId, value);
    }
    
    protected int getStackSize(int i) {
        return Thread.currentThread().getStackTrace().length - i;
    }

    long nextObjectId() {
        return nextObjectId++;
    }

    
}
