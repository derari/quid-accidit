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
        out.println(String.format("V %s %02d $s $s", 
                var.getMethod(), var.getId(), var.getType(), var.getName()));
    }
    
    @Override
    public void traceHead(ThreadTrace trace) {
        out.println(trace.getName() + " ------------------------------------");
    }

    @Override
    public void traceObject(ObjectTrace object) {
        out.println("O " + object);
    }
    
    @Override
    public void traceContent(ThreadTrace trace) {
        invocation(trace.getRoot());
    }
    
    private void indent(int depth) {
        for (int i = 0; i < depth; i++)
            out.print("  ");
    }

    protected void invocation(InvocationTrace invocation) {
        indent(invocation.getDepth());
        out.println(invocation);
        for (InvocationTrace i: invocation.getInvocations()) {
            invocation(i);
        }
    }
    
}
