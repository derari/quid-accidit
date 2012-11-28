package de.hpi.accidit.out;

import de.hpi.accidit.model.*;
import de.hpi.accidit.trace.ObjectTrace;
import de.hpi.accidit.trace.ThreadTrace;

/**
 *
 * @author Arian Treffer
 */
public interface Out {

    public void type(TypeDescriptor type);
    
    public void method(MethodDescriptor method);

    public void variable(VarDescriptor var);

    
    public void traceHead(ThreadTrace trace);

    public void traceObject(ObjectTrace object);
    
    public void traceContent(ThreadTrace trace);

}
