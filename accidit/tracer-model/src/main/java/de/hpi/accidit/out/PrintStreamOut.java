package de.hpi.accidit.out;

import de.hpi.accidit.model.*;
import de.hpi.accidit.trace.*;
import java.io.PrintStream;

/**
 *
 * @author Arian Treffer
 */
public class PrintStreamOut implements Out {

    private final PrintStream out;

    public PrintStreamOut(PrintStream out) {
        this.out = out;
    }

    public PrintStreamOut() {
        this(System.out);
    }

    @Override
    public void type(TypeDescriptor type) {
        out.println("T " + type);
    }

    @Override
    public void method(MethodDescriptor method) {
        out.println("M " + method);
    }

    @Override
    public void variable(VarDescriptor var) {
        out.println(String.format("V %s %s", 
                var.getMethod(), var));
    }

    @Override
    public void field(FieldDescriptor field) {
        out.println("F " + field);
    }
    
    @Override
    public void begin(ThreadTrace trace) {
        out.println(trace.getName() + " ------------------------------------");
    }

    @Override
    public void traceObject(ThreadTrace trace, ObjectTrace object) {
        object.getId();
        out.printf("%2d O %s\n", trace.getId(), object);
    }
    
    private void indent(int depth) {
        for (int i = 0; i < depth; i++)
            out.print("  ");
    }

    @Override
    public void traceCall(CallTrace call) {
        printStep(call.getTrace().getId(), call.getStep(), call.getLine(), call.getDepth()-1);
//        indent(call.getDepth());
        out.println(call);
    }

    @Override
    public void traceExit(CallTrace call, ExitTrace exit) {
        printStep(call.getTrace().getId(), exit.getStep(), exit.getLine(), call.getDepth());
//        indent(call.getDepth());
        out.println(exit + "    " + call);
    }

    @Override
    public void traceThrow(CallTrace call, ThrowableTrace exTrace) {
        printStep(call.getTrace().getId(), exTrace.getStep(), exTrace.getLine(), call.getDepth());
//        indent(call.getDepth());
        out.println("!! " + exTrace);
    }

    @Override
    public void traceCatch(CallTrace call, ThrowableTrace exTrace) {
        printStep(call.getTrace().getId(), exTrace.getStep(), exTrace.getLine(), call.getDepth());
//        indent(call.getDepth());
        out.println("{{ " + exTrace);
    }

    @Override
    public void traceVariable(CallTrace call, VariableTrace var) {
        printStep(call.getTrace().getId(), var.getStep(), var.getLine(), call.getDepth());
//        indent(call.getDepth());
        out.println(var);
    }

    @Override
    public void traceGet(CallTrace call, FieldTrace field) {
        printStep(call.getTrace().getId(), field.getStep(), field.getLine(), call.getDepth());
//        indent(call.getDepth());
        out.println("GET " + field);
    }

    @Override
    public void tracePut(CallTrace call, FieldTrace field) {
        printStep(call.getTrace().getId(), field.getStep(), field.getLine(), call.getDepth());
//        indent(call.getDepth());
        out.println("PUT " + field);
    }

    @Override
    public void traceExistingField(int traceId, FieldTrace field) {
        printStep(traceId, field.getStep(), field.getLine(), -1);
        out.println(">>> " + field);
    }

    @Override
    public void traceArrayGet(CallTrace call, ArrayItemTrace array) {
        printStep(call.getTrace().getId(), array.getStep(), array.getLine(), call.getDepth());
//        indent(call.getDepth());
        out.println("GET " + array);
    }

    @Override
    public void traceArrayPut(CallTrace call, ArrayItemTrace array) {
        printStep(call.getTrace().getId(), array.getStep(), array.getLine(), call.getDepth());
//        indent(call.getDepth());
        out.println("PUT " + array);
    }

    @Override
    public void traceExistingItem(int traceId, ArrayItemTrace array) {
        printStep(traceId, array.getStep(), array.getLine(), -1);
//        indent(call.getDepth());
        out.println(">>> " + array);    }

    @Override
    public void end(ThreadTrace trace) {
        out.println("------------------------------------");
    }
    
    private void printStep(int traceId, long step, int line, int indent) {
        indent = indent*2+5;
        String s = line < 0 ? " " : line + " ";
        out.printf("%2d %6d:%-"+indent+"s", traceId, step, s);
    }

}
