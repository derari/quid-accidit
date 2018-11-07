package de.hpi.accidit.trace;

import de.hpi.accidit.model.MethodDescriptor;
import de.hpi.accidit.model.Model;
import de.hpi.accidit.model.PrimitiveType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author Arian Treffer
 */
public class ThreadTrace {
    
    private static final boolean LOG = true;
    private static final long LOG_SMALL = 5000;
    private static final long LOG_STACK = LOG_SMALL*10;
    private static final long TRACE_MAX = LOG_STACK*20;
    
    private static final int MAX_ERROR = 10;
    private int errorCount = 0;

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
    
    private long logSmall = LOG_SMALL;
    private long logStack = LOG_STACK;

    public ThreadTrace(Model model, int id, EndCallback endCallback) {
        this.model = model;
        this.id = id;
        this.endCallback = endCallback;
        runningTraces.add(this);
    }
    
    @SuppressWarnings("CallToThreadDumpStack")
    private void report(String msg, Throwable t) {
        boolean end = false;
        errorCount++;
        msg += " (" + errorCount + "/" + MAX_ERROR + ")";
        if (invocation != null) {
            msg += " " + invocation;
        }
        if (errorCount >= MAX_ERROR) {
            cancelTrace();
            end = true;
            msg += " trace canceled";
            System.out.println(msg);
        } else if (invocation == null && root != null) {
            // we already left the traced part,
            // clean up and try to do no harm
            cancelTrace();
            errorCount = 1000 + MAX_ERROR;
            return;
//            msg += " trace ended";
        }
        StackTraceElement[] errorStack = t.getStackTrace();
        if (errorStack != null && errorStack.length > 0) {
            System.out.println(t.getMessage() + " " + errorStack[0]);
        } else {
            System.out.println(t.getMessage());
        }
        if (end) throw new AssertionError("end trace: " + msg, t);
        //new RuntimeException(msg, t).printStackTrace();
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
    
    public ObjectTrace getObjectTrace(Object object) {
        return getObjectTrace(object, false);
    }
    
    public synchronized ObjectTrace getObjectTrace(Object object, boolean newObject) {
        if (object == null) return null;
        ObjectTrace ot = objects.get(object);
        if (ot == null) {
            ot = new ObjectTrace(this, object, newObject);
            objects.put(object, ot);
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
        if (object == null) return 0;
        ObjectTrace ot = getObjectTrace(object);
        ot.ensurePersisted();
        return ot.getId();
    }

    public void end() {
        if (cflow) return;
        cflow = true;
        try {
            if (getStackSize(1) <= baseStack) {
                cancelTrace();
            }
        } catch (RuntimeException | Error e) {
//            e.printStackTrace();
//            throw e;
            report("end", e);
        } finally {
            runningTraces.remove(this);
            cflow = false;
        }
    }
    
    private void cancelTrace() {
//        System.out.println("Ending trace " + id + " (" + invocation);
        runningTraces.remove(this);
        Error e = null;
        if (invocation != null) {
            e = new Error("Trace canceled");
            ObjectTrace ot = getObjectTrace(e);
            invocation.cancel(ot);
        }
        model.out.end(this);
        endCallback.ended(this);
//        if (e != null) {
//            Thread.currentThread().interrupt();
//            throw e;
//        }
    }
    
    /** line number before method invocation */
    public void line(int line) {
        if (cflow) return;
        cflow = true;
        try {
            invocation.line(line);
        } catch (RuntimeException | Error e) {
            report("line " + line, e);
        } finally {
            cflow = false;
        }
    }
    
    public void call(int methodCode) {
        if (cflow) return;
        cflow = true;
        try {
            invocation.call(methodCode);
        } catch (RuntimeException | Error e) {
            report(invocation + " call-> " + model.getMethod(methodCode), e);
        } finally {
            cflow = false;
        }
    }
    
    public void enter(int methodCode, Object instance, boolean newObject) {
        if (cflow) return;
        cflow = true;
        MethodDescriptor method = null;
        try {
            method = model.getMethod(methodCode);
            if (invocation == null) {
//                boolean newObject = method.getName().equals("<init>");
                ObjectTrace otInstance = getObjectTrace(instance, newObject);
                invocation = new CallTrace(null, this, method, otInstance, step, -1, 0, false);
                assert root == null : "Should not have traced yet";
                root = invocation;
                name = method.getOwner().getName() + "#" + method.getName() + method.getDescriptor();
                baseStack = getStackSize(1);
                model.out.begin(this);
                root.ensurePersisted();
            } else {
                invocation = invocation.enter(method, instance);
                if (invocation == null) {
                    AssertionError e = new AssertionError("`enter` did not return invocation");
                    e.printStackTrace(System.err);
                    //throw e;
                    cancelTrace();
                    return;
                }
            }
            stack.push(invocation);
            long s = step.current();
            if (s > logSmall) {
                logSmall += LOG_SMALL;
                System.out.print('.');
                if (s > logStack) {
                    logStack += LOG_STACK;
                    System.out.println();
                    int i = 0;
                    for (CallTrace c: stack) {
                        if (c.log()) {
                            if (i++ > 3) break;
                        }
                    }
                    if (s > TRACE_MAX) {
                        System.out.println("--- enough ---");
                        cancelTrace();
                        throw new AssertionError("enough---");
                    }
                }
            }
        } catch (RuntimeException | Error e) {
            report("enter " + instance + "." + method, e);
        } finally {
            cflow = false;
        }
    }
    
//    void fillStack(int start, int height) {
//        for (int i = start; i <= height; i++) {
//            stack.push(new CallTrace(this, model.nullMethod(), null, step, -1, i, true));
//        }
//    }

    private static final Error NO_RETURN = new Error("NO RETURN");
    
    public void returned(int methodCode, int line, Object result) {
        if (cflow) return;
        cflow = true;
        try {
            if (invocation.returned(methodCode, line, result)) {
                stack.pop();
            } else {
                final ObjectTrace otNoReturn = getObjectTrace(NO_RETURN);
                int depth = getCurrentDepth(1);
                while (invocation.getDepth() > depth) {
                    invocation.failed(otNoReturn);
                    stack.pop();
                    if (stack.isEmpty()) {
                        cancelTrace();
                        return;
                    }
                    invocation = stack.peek();
                }
                if (depth == invocation.getDepth()) {
                    if (!invocation.returned(methodCode, line, result)) {
                        invocation.failed(otNoReturn);
                        //throw new IllegalStateException(invocation.toString());
                    }
                    stack.pop();
                }
            }
            if (stack.isEmpty()) {
                invocation = null;
            } else {
                invocation = stack.peek();
            }
        } catch (RuntimeException | Error e) {
            report(line + ": return " + invocation + " (" + methodCode + "): " + result, e);
        } finally {
            cflow = false;
        }
    }
    
    public void thrown(int line, Object t) {
        if (cflow) return;
        cflow = true;
        try {
            invocation.thrown(line, t);
        } catch (RuntimeException | Error e) {
            report(line + ": throw " + t, e);
        } finally {
            cflow = false;
        }
    }
    
    public void caught(int line, Object t) {
        if (cflow) return;
        cflow = true;
        try {
            ObjectTrace ot = getObjectTrace(t);
            invocation.ensureThrown(ot);
            int depth = getCurrentDepth(1);
            while (invocation.getDepth() > depth) {
                stack.pop().failed(ot);
                if (stack.isEmpty()) {
                    cancelTrace();
                    return;
                }
                invocation = stack.peek();
            }
            invocation.caught(line, ot);
        } catch (RuntimeException | Error e) {
            report(line + ": catch " + t, e);
        } finally {
            cflow = false;
        }
    }
    
    public void argument(int argId, Object value) {
        if (cflow) return;
        cflow = true;
        try {
            invocation.argument(argId, value);
        } catch (RuntimeException | Error e) {
            report("arg " + argId + " = " + value, e);
        } finally {
            cflow = false;
        }
    }
    
    public void variable(int line, int offset, int varId, Object value) {
        if (cflow) return;
        cflow = true;
        try {
            invocation.variable(line, offset, varId, value);
        } catch (RuntimeException | Error e) {
            report(line + ": var " + varId + " = " + value, e);
        } finally {
            cflow = false;
        }
    }

    public void put(Object instance, Object value, int fieldId, int line) {
        if (cflow) return;
        cflow = true;
        try {
            invocation.put(line, instance, fieldId, value);
        } catch (RuntimeException | Error e) {
            report(line + ": " + instance + "." + fieldId + " <- " + value, e);
        } finally {
            cflow = false;
        }
    }
    
    public void get(Object instance, Object value, int fieldId, int line) {
        if (cflow) return;
        cflow = true;
        try {
            invocation.get(line, instance, fieldId, value);
        } catch (RuntimeException | Error e) {
            report(line + ": " + instance + "." + fieldId + " -> " + value, e);
        } finally {
            cflow = false;
        }
    }
    
    public void arrayStore(Object instance, Object value, int index, int line) {
        if (cflow) return;
        cflow = true;
        try {
            invocation.arrayStore(line, instance, index, value);
        } catch (RuntimeException | Error e) {
            report(line + ": " + instance + "[" + index + "] <- " + value, e);
        } finally {
            cflow = false;
        }
    }
    
    public void arrayLoad(Object instance, Object value, int index, int line) {
        if (cflow) return;
        cflow = true;
        try {
            invocation.arrayLoad(line, instance, index, value);
        } catch (RuntimeException | Error e) {
            report(line + ": " + instance + "[" + index + "] <- " + value, e);
        } finally {
            cflow = false;
        }
    }
    
    protected int getCurrentDepth(int i) {
        return getStackSize(i+1) - baseStack;
    }
    
    protected static int getStackSize(int i) {
        return Thread.currentThread().getStackTrace().length - i;
    }

    long nextObjectId() {
        return nextObjectId++;
    }

    boolean sanatizeStack() {
        StackTraceElement[] actualStack = Thread.currentThread().getStackTrace();
        int bottomFrame = actualStack.length - baseStack + 3;
        for (int i = 0; i < stack.size(); i++) {
            CallTrace call = stack.get(i);
            int fId = bottomFrame - call.getDepth();
            if (fId < 5 || // fId < 5 is already in Tracer code
                    fId >= actualStack.length ||
                    !actualStack[fId].getMethodName().equals(call.getMethod().getName())) {
                while (stack.size() > i) stack.pop();
                if (stack.isEmpty()) {
                    end();
                    return false;
                }
                invocation = stack.peek();
                return true;
            }
        }
        return true;
    }

    /** for testing */
    public void failFastMode() {
        errorCount = MAX_ERROR;
    }

    public static interface EndCallback {
        
        void ended(ThreadTrace trace);
    }
    
    private static final List<ThreadTrace> runningTraces = Collections.synchronizedList(new ArrayList<ThreadTrace>());
    
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                synchronized (Tracer.class) {
                    Tracer.pauseTrace();
                }
                for (ThreadTrace tt: new ArrayList<>(runningTraces)) {
                    tt.cancelTrace();
                }
            }
        });
    }
}
