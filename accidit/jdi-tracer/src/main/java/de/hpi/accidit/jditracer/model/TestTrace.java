package de.hpi.accidit.jditracer.model;

import com.sun.jdi.*;
import de.hpi.accidit.jditracer.FrameHandler;
import java.util.*;

/**
 *
 * @author Arian Treffer
 */
public class TestTrace {
    
    private final Model model;
    private final int id;
    private final String name;
    private final long startMs = System.currentTimeMillis();

    private List<InvocationTrace> invocations = new ArrayList<>(1024);
    private Map<Long, ObjectTrace> objects = new HashMap<>(1024);
    private int step = 0;
    private int nextTraceStep = 2000;
    
    private long nextObjectId = 1;
    
    public TestTrace(Model model, int id, Method m, ThreadReference t) throws IncompatibleThreadStateException {
        StringBuilder sb = new StringBuilder();
        sb.append(m.declaringType().name());
        sb.append('#');
        sb.append(m.name());
        sb.append('(');
        boolean first = true;
        for (Value v: t.frame(0).getArgumentValues()) {
            if (first) first = false;
            else sb.append(',');
            sb.append(v);
        }
        sb.append(')');
        
        this.model = model;
        this.name = sb.toString();
        this.id = id;
        model.out.test(this);
        System.out.println();
        System.out.print(name);
    }
    
    public void continueStep() {
        step--;
    }
    
    private ObjectTrace getObject(ObjectReference obj) {
        if (obj == null) return null;
        ObjectTrace t = objects.get(obj.uniqueID());
        if (t == null) {
//            if (objects.size() > objectsMapCapacity) {
//                Iterator<ObjectReference> it = objects.keySet().iterator();
//                while (it.hasNext()) {
//                    ObjectReference o = it.next();
//                    if (o.isCollected()) it.remove();
//                }
//                while (objects.size() > objectsMapCapacity) objectsMapCapacity *= 2;
//            }
            t = new ObjectTrace(model, model.getType(obj.type()), id, nextObjectId++);
            objects.put(obj.uniqueID(), t);
            
        }
        return t;
    }

    private void fillValue(ValueTrace trace, Value value) {
        trace.step = step++;
        int primType = PrimitiveDescriptor.primitiveTypeId(value);
        trace.primitiveId = primType;
        if (primType == 0) {
            if (value == null || value instanceof NullValue)  {
                trace.value = 0;
            } else {
                ObjectReference obj = (ObjectReference) value;   
                ObjectTrace ot = getObject(obj);
                ot.ensureIsTraced();
                trace.value = ot.getId();
            }
        } else {
            trace.value = PrimitiveDescriptor.primitiveData(value, primType);
        }
    }
    
    private void fillLocation(LocationTrace trace, Location loc) {
//        try {
//            trace.source = model.getSource(loc.sourcePath());
//        } catch (AbsentInformationException ex) {
//            trace.source = model.getSource("unknown");
//        }
        trace.line = loc.lineNumber();
    }
    
    private final List<FrameHandler> lastPrintedFrames = new ArrayList<>();
    
    public InvocationTrace invoke(final Method method, final int depth, final int callLine, final ObjectReference thisObject) {
        while (step > nextTraceStep) {
            System.out.print(".");
            if (nextTraceStep % 20000 == 0) {
                System.out.println();
                int i = 0;
                int m = Math.min(frames.size(), lastPrintedFrames.size());
                while (i < m && lastPrintedFrames.get(i) == frames.get(i)) {
                    System.out.print("/");
                    i++;
                }
                for (int r = lastPrintedFrames.size()-1; r >= i; r--)
                    lastPrintedFrames.remove(r);
                m = Math.min(i+5, frames.size());
                for (;i < m; i++) {
                    FrameHandler f = frames.get(i);
                    f.printLastStep();
                    lastPrintedFrames.add(f);
                }
            }
            nextTraceStep += 2000;
        }
        
        ObjectTrace thisObjTrace = getObject(thisObject);
        InvocationTrace invoke = new InvocationTrace(id, model.getMethod(method), step++, depth, callLine, thisObjTrace);
        invocations.add(invoke);
        return invoke;
    }

    public void exit(InvocationTrace invoke, Value retVal, Location loc) {
        fillValue(invoke, retVal); // increments the step
        fillLocation(invoke, loc);
        invoke.exit(step, true);
    }
    
    public void exceptionExit(InvocationTrace invoke, ObjectReference exception, Location loc) {
        fillValue(invoke, exception); // increments the step
        fillLocation(invoke, loc);
        invoke.exit(step, false);
    }
    
    public void throwException(InvocationTrace invoke, ObjectReference exception, Location loc) {
        ObjectTrace exTrace = getObject(exception);
        ThrowTrace trace = new ThrowTrace(step++, exTrace);
        fillLocation(trace, loc);
        invoke.exThrow(trace);
    }
    
    public void catchException(InvocationTrace invoke, ObjectReference exception, Location loc) {
        ObjectTrace exTrace = getObject(exception);
        CatchTrace trace = new CatchTrace(step++, exTrace);
        fillLocation(trace, loc);
        invoke.exCatch(trace);
    }
    
    public void setLocal(final InvocationTrace invoke, final LocalVariable lv, final Value newV, final Location loc) {
//        HandlerThread.run(new Runnable() {
//            @Override public void run() {
//                doSetLocal(invoke, lv, newV, loc);
//            }
//        });
//    }
//        
//    private void doSetLocal(InvocationTrace invoke, LocalVariable lv, Value newV, Location loc) {
        LocalVarDescriptor lvDesc = invoke.getMethod().getLocal(lv);
        if (!lvDesc.tryInitialize()) {
            if (newV == null || newV instanceof NullValue) {
                return; // will be initialized when there is a value
            }
            // there is a value, but the variable type is not initialized??
            // load type from value, then try again
            model.getType(newV.type());
            lvDesc.requireInitialized();
        }
        
        LocalVarTrace lvTrace = new LocalVarTrace(lvDesc);
        fillValue(lvTrace, newV);
        fillLocation(lvTrace, loc);
        invoke.setLocal(lvTrace);
    }
    
    public void modification(final InvocationTrace invoke, final ObjectReference obj, final Field field, final Value value, final Location loc) {
//        HandlerThread.run(new Runnable() {
//            @Override public void run() {
//                doModification(invoke, obj, field, value, loc);
//            }
//        });
//    }
//        
//    private void doModification(InvocationTrace invoke, ObjectReference obj, Field field, Value value, Location loc) {
        ObjectTrace o = getObject(obj);
        FieldDescriptor f = model.getField(field);
        if (f.tryInitialize()) {
            FieldTrace fTrace = new FieldTrace(o, f);
            fillValue(fTrace, value);
            fillLocation(fTrace, loc);
            invoke.modification(fTrace);
        } else if (value != null) {
            throw new IllegalStateException(f + " not initialized");
        }
    }

    public void access(final InvocationTrace invoke, final ObjectReference obj, final Field field, final Location loc) {
//        HandlerThread.run(new Runnable() {
//            @Override public void run() {
//                doAccess(invoke, obj, field, loc);
//            }
//        });
//    }
//        
//    private void doAccess(InvocationTrace invoke, ObjectReference obj, Field field, Location loc) {
        ObjectTrace o = getObject(obj);
        FieldDescriptor f = model.getField(field);
//        if (!f.tryInitialize()) {
//            initializeFieldDescriptor(f, field);
//        }
        f.requireInitialized();
        FieldAccessTrace fTrace = new FieldAccessTrace(step++, o, f);
        fillLocation(fTrace, loc);
        invoke.access(fTrace);
    }
    
//    private void initializeFieldDescriptor(FieldDescriptor f, Field field) {
//        List<ReferenceType> types = field.virtualMachine().classesByName(field.typeName());
//        if (!types.isEmpty()) {
//            for (Type t:  types) {
//                model.getType(t);
//            }
//            if (f.tryInitialize()) return;
//        }
//        model.createTypeDummy(field.typeName(), field.virtualMachine());
//        if (f.tryInitialize()) return;
//        throw new IllegalStateException("Cannot initialize " + f);
//    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<InvocationTrace> getInvocations() {
        return invocations;
    }

    public void complete() {
//        HandlerThread.run(new Runnable() {
//            @Override public void run() {
//                doComplete();
//            }
//        });
//    }
//    
//    private void doComplete() {
        model.out.testData(this);
        long now = System.currentTimeMillis();
        double testS = (now - startMs) / 1000.0;
        double totalS = (now - ms) / 1000.0;
//        System.out.println(name + " completed (" + s + " s)");
        System.out.printf(Locale.US, " completed (%d steps, %.1f s, %.1f s total)", step, testS, totalS);
        frames = null;
        objects = null;
        invocations = null;
    }

    private static final long ms = System.currentTimeMillis();
    
    public void dump() {
        System.out.println();
        System.out.println(id + " " + name + " -----------------------------------------");
        for (InvocationTrace inv: invocations) {
            inv.dump();
        }
    }
    
    private List<FrameHandler> frames;

    public void setRoot(List<FrameHandler> frames) {
        this.frames = frames;
    }

}
