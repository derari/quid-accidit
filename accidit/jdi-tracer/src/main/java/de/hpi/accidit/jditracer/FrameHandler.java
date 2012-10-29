package de.hpi.accidit.jditracer;

import de.hpi.accidit.jditracer.model.NullValue;
import de.hpi.accidit.jditracer.model.TestTrace;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import de.hpi.accidit.jditracer.model.InvocationTrace;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Arian Treffer
 */
public class FrameHandler {
    
    private static final boolean PRINT_TRACE = false;
    
    private final TestTrace trace;
    private final Method method;
    private final int depth;
    
    private boolean ignoreDetails;
    private InvocationTrace invoke;
    
    private final Map<String, Value> variableMap = new HashMap<>();
    private boolean traceVariables = true;
    
    private int lastPrintedLine = -1;
    private Location lastLocation;
    private int lastStepLine = -1;
    
    private int stepCounter = 0;
    
    public FrameHandler(LocatableEvent event, Method m, int depth, int callLine, TestTrace trace) throws IncompatibleThreadStateException {
        this.method = m;
        this.depth = depth;
        this.trace = trace;
    }
    
    private void indent() {
        System.out.print("\n");
        for (int i = 0; i < depth; i++) {
            System.out.print("  ");
        }
    }
    
    public void returnBack() {
        lastPrintedLine = -1;
    }
    
    private void checkEvent(LocatableEvent event) {
        try {
            if (!event.location().sourceName().equals(method.location().sourceName())) {
                throw new IllegalArgumentException(
                    method + " -- " + event.location().method() + " / " + event + " @ " + event.location());
            }
        } catch(AbsentInformationException e) {}
    }
    
    public void entry(LocatableEvent event, Method m, int callLine) throws IncompatibleThreadStateException {        
        //checkEvent(event);
        lastLocation = event.location();
        final StackFrame sf = event.thread().frame(0);
        
        ObjectReference self = sf.thisObject();
        invoke = trace.invoke(m, depth, callLine, self);
        ignoreDetails = invoke.ignoreInternals();

        if (PRINT_TRACE) {
            indent();
            System.out.print("> ");
            if (self != null) {
                System.out.print(self.type().name() + "_" + self.uniqueID() + " ");
            } else {
                System.out.print(m.declaringType().name() + " ");
            }
            System.out.print(m.name());
        }
        
        if (method.isNative()) {
            traceVariables = false;
            return;
        }
        
        traceVariables(event, true);
    }
    
    public boolean ignoresDetails(boolean parentIgnores) {
        
        return false;
    }
    
    public void errorExit() {
        trace.exit(invoke, null, lastLocation);
    }
    
    public void exit(MethodExitEvent mex) {
        if (PRINT_TRACE) {
            indent();
            System.out.print("  < " + mex.returnValue());
        }
        trace.exit(invoke, mex.returnValue(), mex.location());
    }

    public void printLastStep() {
        System.out.print(" ");
        try {
            System.out.print(lastLocation.sourceName());
//            System.out.print(method.name());
        } catch (Exception ex) {
            System.out.print("???");
        }
        System.out.print(":");
        System.out.print(lastLocation.lineNumber());
    }

    public boolean isTraceVariables() {
        return traceVariables;
    }
    
    private int retry = 3;
    
    public void step(StepEvent step) throws IncompatibleThreadStateException {
        stepCounter++;
        if (stepCounter >= 10000 && stepCounter % 1000 == 0) {
            traceVariables = false;
        }
        if (!method.equals(step.location().method())) {
            if (retry-- > 0) return;
            throw new IllegalTracerStateException(
                    method + " -- " + step.location().method() + " / " + step);
        }
        if (PRINT_TRACE) {
            int line = step.location().lineNumber();
            if (line != lastPrintedLine) {
                lastPrintedLine = line;
                indent();
                try {
                    System.out.print("  " + step.location().sourceName());
                } catch (AbsentInformationException ex) {
                    System.out.print("  " + "???");
                }
                System.out.print(":");
                System.out.print(step.location().lineNumber());
            }
            System.out.print(".");
        }
        //checkEvent(step);
        Location loc = step.location();
        lastLocation = loc;
        if (lastStepLine > -2) {
            int curLine = loc.lineNumber();
            if (lastStepLine == curLine) return;
            lastStepLine = curLine;
        }
        traceVariables(step, false);        
    }

    private void traceVariables(LocatableEvent event, boolean continueStep) throws IncompatibleThreadStateException {
        if (!traceVariables) return;
        try {
            StackFrame sf = event.thread().frame(0);
//                for (LocalVariable lv: sf.visibleVariables()) {
//                    Value newV = sf.getValue(lv);
            Map<LocalVariable, Value> values = sf.getValues(sf.visibleVariables());
            for (Map.Entry<LocalVariable, Value> e: values.entrySet()) {
                LocalVariable lv = e.getKey();
                Value newV = e.getValue();
                Value oldV = variableMap.get(lv.name());
                if (newV == null) newV = NullValue.NULL;
                if (!newV.equals(oldV)) {
                    if (PRINT_TRACE) {
                        lastPrintedLine = -1;
                        indent();
                        System.out.print("  - " + lv.name() + " = ");
                        System.out.print(newV);
                    }
                    variableMap.put(lv.name(), newV);
                    if (continueStep) trace.continueStep();
                    else continueStep = true;
                    trace.setLocal(invoke, lv, newV, event.location());
                }
            }
        } catch (AbsentInformationException ex) {
            traceVariables = false;
//                lastLine = -1;
//                indent();
//                System.out.print(ex);
        }
        
//        try {
//            int cIndex = (int) step.location().codeIndex();
//            byte[] bytes = method.bytecodes();
//            byte b = bytes[cIndex];
//            if (b >= 0x4f && b <= 0x56) {
//                lastLine = -1;
//                indent();
//                System.out.print("  array store " + Integer.toHexString(b) + " ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~+");
//                ArrayReference r = null;
//                r.getValues();
//            }
//        } catch (Exception e) {}
    }

    public void modification(ModificationWatchpointEvent mod) {
        ObjectReference obj = mod.object();
        Field f = mod.field();
        if (PRINT_TRACE) {
            lastPrintedLine = -1;
            indent();

            String objString;
            if (obj == null) {
                objString = f.declaringType().name();
            } else {
                objString = obj.type().name() + "_" + obj.uniqueID();
            }

            System.out.print("   " + objString + "." +
                    f.name() + " = " + mod.valueToBe());
        }
        
        trace.modification(invoke, obj, f, mod.valueToBe(), mod.location());
    }

    public void access(AccessWatchpointEvent mod) {
        ObjectReference obj = mod.object();
        Field f = mod.field();
        if (PRINT_TRACE) {
            lastPrintedLine = -1;
            indent();

            String objString;
            if (obj == null) {
                objString = f.declaringType().name();
            } else {
                objString = obj.type().name() + "_" + obj.uniqueID();
            }

            System.out.print("   " + objString + "." +
                    f.name() + " ->");
        }
        
        trace.access(invoke, obj, f, mod.location());
    }

    public void exception(ObjectReference exception) {
        if (PRINT_TRACE) {
            lastPrintedLine = -1;
            indent();
            System.out.print("  <!");
        }
        trace.exceptionExit(invoke, exception, lastLocation);
    }

    public void catchException(ObjectReference exception, Location catchLocation) {
        if (PRINT_TRACE) {
            lastPrintedLine = -1;
            indent();
            System.out.print("  { " + exception);
        }
        trace.catchException(invoke, exception, catchLocation);
    }

    public void throwException(ExceptionEvent exe) {
        if (PRINT_TRACE) {
            lastPrintedLine = -1;
            indent();
            System.out.print("  ! " + exe.exception());
        }
        lastLocation = exe.location();
        trace.throwException(invoke, exe.exception(), exe.location());
    }

    public int getLastStepLine() {
        return lastStepLine;
    }

    @Override
    public String toString() {
        return method + ":" + lastStepLine;
    }

}
