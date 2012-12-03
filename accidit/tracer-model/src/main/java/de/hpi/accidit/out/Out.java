package de.hpi.accidit.out;

import de.hpi.accidit.model.*;
import de.hpi.accidit.trace.*;

/**
 *
 * @author Arian Treffer
 */
public interface Out {

    public void type(TypeDescriptor type);
    
    public void method(MethodDescriptor method);

    public void variable(VarDescriptor var);

    public void field(FieldDescriptor field);

    
    public void begin(ThreadTrace trace);

    public void traceObject(ThreadTrace trace, ObjectTrace object);
    
    public void traceCall(CallTrace call);

    public void traceExit(CallTrace call, ExitTrace exit);

    public void traceThrow(CallTrace call, ThrowableTrace exTrace);

    public void traceCatch(CallTrace call, ThrowableTrace exTrace);

    public void traceVariable(CallTrace call, VariableTrace var);
    
    public void tracePut(CallTrace call, FieldTrace field);

    public void traceGet(CallTrace call, FieldTrace field);

    public void traceArrayPut(CallTrace call, ArrayItemTrace array);

    public void traceArrayGet(CallTrace call, ArrayItemTrace array);

    public void end(ThreadTrace trace);

}
