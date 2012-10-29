package de.hpi.accidit.jditracer;

import de.hpi.accidit.jditracer.model.Trace;
import de.hpi.accidit.jditracer.model.TestTrace;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.Stack;

/**
 *
 * @author Arian Treffer
 */
public class ThreadHandler {
    
    public static boolean triggerGC = false;
    
    // hard ignore, no tracing below this point
    private static final String[][] IGNORE = new String[][]{
        {"java.lang.ClassLoader"},        {""},//{"loadClass", "checkPackageAccess"},
        {"java.io.OutputStreamClass"},    {""},
        {"sun."},                         {""},
        {"java.util.HashMap"},            {""},
        {"$", "java.lang.reflect"},
            {""},
    };
    
    private final Trace trace;
    private TestTrace testTrace = null;
    
    private final EventRequestManager erm;
    private final GlobalFieldWatcher.T fieldWatcher;
    private final StepRequest stepRequest;
    private final MethodEntryRequest entryRequest;
    private final MethodExitRequest exitRequest;
    private final ExceptionRequest exceptionRequest;
    private final Stack<FrameHandler> frames = new Stack<>();
    private MethodExitRequest ignoreExitRequest;
    
    private boolean active;
    private String lastTestClass;
    
    private int ignoreBaseFrame = 0;
    private int baseFrame = -1;
    private ObjectReference toBeCaught = null;
    private Location catchLocation = null;
    private ThreadReference thread = null;
    private boolean enableSteps = true;
    
    private int stepCount = 0;
    private int enterCount = 0;
    private int modAccCount = 0;
    private int otherCount = 0;
    private int ignoreCount = 0;
    private int ignore2Count = 0;
    
    public ThreadHandler(ThreadReference thread, GlobalFieldWatcher fieldWatcher, Trace trace) {
        this.trace = trace;
        this.fieldWatcher = fieldWatcher.getT(thread);
        erm = thread.virtualMachine().eventRequestManager();
        stepRequest = erm.createStepRequest(thread, StepRequest.STEP_MIN, StepRequest.STEP_INTO);
        entryRequest = erm.createMethodEntryRequest();
        entryRequest.addThreadFilter(thread);
        exitRequest = erm.createMethodExitRequest();
        exitRequest.addThreadFilter(thread);
        
        exceptionRequest = erm.createExceptionRequest(null, true, true);
        exceptionRequest.addThreadFilter(thread);
    }
    
    static int hId = 0;
    int myHID = hId++;
    
    public void handle(LocatableEvent event) throws IncompatibleThreadStateException {
        try {
    //        System.out.println(myHID);
            if (toBeCaught != null) {
                if (!handleCatch(event)) {
                    if (event.request() != ignoreExitRequest) return;
                }
                if (frames.isEmpty()) return;
            }

            if (event instanceof StepEvent) {
                if (ignoreBaseFrame > 0) ignore2Count++;
                stepCount++;
                handleStep((StepEvent) event);

            } else if (event instanceof ModificationWatchpointEvent) {
                if (ignoreBaseFrame > 0) ignore2Count++;
                modAccCount++;
                handleModificationWatchpoint((ModificationWatchpointEvent) event);

            } else if (event instanceof AccessWatchpointEvent) {
                if (ignoreBaseFrame > 0) ignore2Count++;
                modAccCount++;
                handleAccessWatchpoint((AccessWatchpointEvent) event);

            } else if (event instanceof MethodEntryEvent) {
                if (ignoreBaseFrame > 0) ignoreCount++;
                enterCount++;
                if (event.request() == entryRequest) {
                    handleMethodEntry(event, ((MethodEntryEvent) event).method());
                } else {
                    activate((MethodEntryEvent) event);
                }

            } else if (event instanceof MethodExitEvent) {
                if (ignoreBaseFrame > 0) ignoreCount++;
                otherCount++;
                handleMethodExit((MethodExitEvent) event);

            } else if (event instanceof ExceptionEvent) {
                if (ignoreBaseFrame > 0) ignoreCount++;
                otherCount++;
                handleException((ExceptionEvent) event);

            } else if (event instanceof BreakpointEvent) {
                if (ignoreBaseFrame > 0) ignoreCount++;
                otherCount++;
                activate(event);
            } else {
                throw new IllegalArgumentException(event.toString());
            }
        } catch (IllegalTracerStateException e) {
            e.printStackTrace();
            deactivate();
        }
    }
    
    private void enableSteps(boolean b) {
        enableSteps = b;
        stepRequest.setEnabled(b);
    }
    
    private void activate(LocatableEvent e) throws IncompatibleThreadStateException {
        if (active) return;
        active = true;
//        stepCount = modAccCount = enterCount = otherCount = ignore2Count = ignoreCount = 0;
        Method m;
        thread = e.thread();
        if (e instanceof MethodEntryEvent) {
            MethodEntryEvent men = (MethodEntryEvent) e;
            String methodName = men.method().name();
            if (!methodName.startsWith("test")) return;
            //if (methodName.endsWith("init>") || methodName.equals("setUp") || methodName.equals("tearDown")) return;
            m = men.method();
        } else {
            m = e.location().method();
        }
        
        fieldWatcher.enable();
        String testClass = m.declaringType().name();
        if (!testClass.equals(lastTestClass)) {
            fieldWatcher.resetAutoDisable();
        }

        JdiTest.threadFreezer.makeActive(thread);
        JdiTest.threadFreezer.activate();
        stepRequest.enable();
        entryRequest.enable();
        exitRequest.enable();
        exceptionRequest.enable();
        try {
            baseFrame = e.thread().frameCount();
            testTrace = trace.createTestTrace(m, thread);
            testTrace.setRoot(frames);
        } catch (IncompatibleThreadStateException ex) {
            throw new RuntimeException(ex);
        }
//        System.out.println(">");
        lastTestClass = testClass;
        handleMethodEntry(e, m);
    }

    private void deactivate() {
        while (!frames.isEmpty()) frames.pop().errorExit();
        active = false;
        stepRequest.disable();
        entryRequest.disable();
        exitRequest.disable();
        fieldWatcher.disable();
        exceptionRequest.disable();
        testTrace.complete();
        testTrace = null;
        JdiTest.threadFreezer.deactivate();
        JdiTest.threadFreezer.makeInactive(thread);
        triggerGC = true;
//        System.out.println("<");
//        System.out.println("  -- step   " + stepCount);
//        System.out.println("  -- modacc " + modAccCount);
//        System.out.println("  -- enter  " + enterCount);
//        System.out.println("  -- other  " + otherCount);
//        System.out.println("  -- ignore " + ignoreCount);
//        System.out.println("  -- ignor2 " + ignore2Count);
    }
    
    private void beginIgnore() {
        if (ignoreExitRequest != null) {
            ignoreExitRequest.disable();
            erm.deleteEventRequest(ignoreExitRequest);
            ignoreExitRequest = null;
        }
        stepRequest.disable();
        entryRequest.disable();
        exitRequest.disable();
        fieldWatcher.beginIgnore();
    }

    private void endIgnore() {
        if (ignoreExitRequest != null) {
            ignoreExitRequest.disable();
            erm.deleteEventRequest(ignoreExitRequest);
            ignoreExitRequest = null;
        }
        stepRequest.enable();
        entryRequest.enable();
        exitRequest.enable();
        fieldWatcher.endIgnore();
    }

    private void handleMethodEntry(LocatableEvent event, Method m) throws IncompatibleThreadStateException {
        if (ignoreBaseFrame > 0)  return;

        String mName = m.name();
        String tName = m.declaringType().name();
        for (int i = 0; i < IGNORE.length; i += 2) {
            boolean typeMatch = false;
            for (String it: IGNORE[i]) {
                if (tName.startsWith(it)) {
                    typeMatch = true; break;
                }
            }
            if (typeMatch) {
                for (String im: IGNORE[i+1]) {
                    if (mName.startsWith(im)) {
                        ignoreBaseFrame = baseFrame + frames.size() + 1;
                        beginIgnore();
                        ignoreExitRequest = erm.createMethodExitRequest();
                        ignoreExitRequest.addThreadFilter(thread);
                        ignoreExitRequest.addClassFilter(m.declaringType());
                        ignoreExitRequest.enable();
                        return;
                    }
                }
            }
        }
        
        int callLine;
        if (frames.isEmpty()) callLine = 0;
        else callLine = frames.peek().getLastStepLine();
        
        FrameHandler f = new FrameHandler(event, m, frames.size(), callLine, testTrace);
        frames.push(f);
        
        f.entry(event, m, callLine);
        enableSteps(f.isTraceVariables());
//        try {
//            if (men.method().isNative()) return;
//            for (Value v: men.thread().frame(0).getArgumentValues()) {
//                if (v != null && v.type() instanceof ArrayType) {
//                    fieldWatcher.arrayTest((ArrayType) v.type());
//                }
//            }
//        } catch (IncompatibleThreadStateException | InternalException ex) {
//        }
    }

    private void handleMethodExit(MethodExitEvent mex) throws IncompatibleThreadStateException {
        if (ignoreBaseFrame > 0) {
            if (mex.thread().frameCount() < ignoreBaseFrame) {
                ignoreBaseFrame = 0;
                endIgnore();
            }
            return;
        }
        
        frames.pop().exit(mex);
        if (frames.isEmpty()) {
            deactivate();
            //System.out.println("<");
        } else {
            frames.peek().returnBack();
            enableSteps(frames.peek().isTraceVariables());
        }
    }

    private void handleStep(StepEvent step) throws IncompatibleThreadStateException {
        if (ignoreBaseFrame > 0 | !active) return;
        frames.peek().step(step);
    }

    private void handleModificationWatchpoint(ModificationWatchpointEvent mod) {
        if (ignoreBaseFrame > 0) {
            fieldWatcher.ignore((WatchpointRequest) mod.request());
            return;
        }
        if (frames.isEmpty()) {
            fieldWatcher.autoDisable((WatchpointRequest) mod.request());
            return;
        }
        frames.peek().modification(mod);
    }

    private void handleAccessWatchpoint(AccessWatchpointEvent mod) {
        if (ignoreBaseFrame > 0) {
            fieldWatcher.ignore((WatchpointRequest) mod.request());
            return;
        }
        if (frames.isEmpty()) {
            fieldWatcher.autoDisable((WatchpointRequest) mod.request());
            return;
        }
        frames.peek().access(mod);
    }

    private void handleException(ExceptionEvent exe) {
        toBeCaught = exe.exception();
        catchLocation = exe.catchLocation();
        if (ignoreBaseFrame > 0 || !enableSteps) stepRequest.enable();
        else frames.peek().throwException(exe);
    }

    private boolean handleCatch(LocatableEvent evt) throws IncompatibleThreadStateException {
        final ObjectReference exception = toBeCaught;
        final Location location = catchLocation;
        boolean ignoreThisCatch = false;

        if (ignoreBaseFrame > 0) {
            if (evt instanceof WatchpointEvent || 
                    evt instanceof MethodExitEvent) return false;
            toBeCaught = null;
            catchLocation = null;       
            ignoreThisCatch = true;

            if (evt.thread().frameCount() < ignoreBaseFrame) {
//                int c = evt.thread().frameCount();
//                for (int i = 0; i < c; i++)
//                    System.out.println("~~ " + evt.thread().frame(i).location());
                ignoreBaseFrame = 0;
                endIgnore();
            }
//            ignoreBaseFrame = curStackSize - frames.size();
            if (ignoreBaseFrame > 0) {
                stepRequest.disable();
                return false;
            }
//            System.out.print("");
        } else {
            toBeCaught = null;
            catchLocation = null;        
        }
        
        final int stackSize;
        if (evt instanceof StepEvent || evt instanceof ExceptionEvent) {
            stackSize = evt.thread().frameCount() - baseFrame + 1;
        } else if (evt instanceof MethodEntryEvent) {
            int frameOffset = 0;
            Location frameLoc = evt.thread().frame(frameOffset).location();
            while (!frameLoc.method().equals(evt.location().method())) {
                frameOffset++;
                frameLoc = evt.thread().frame(frameOffset).location();
            }
            stackSize = evt.thread().frameCount() - baseFrame - frameOffset;            
        } else {
            throw new IllegalArgumentException(evt.toString());
        }

        while (!frames.isEmpty() && frames.size() > stackSize) {
            frames.pop().exception(exception);
        }
        if (frames.isEmpty()) {
            deactivate();
        } else {
            frames.peek().catchException(exception, location);
            enableSteps(frames.peek().isTraceVariables());
        }
        
        return !ignoreThisCatch;
    }
    
}
