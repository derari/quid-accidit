package de.hpi.accidit.trace;

import de.hpi.accidit.model.PrimitiveType;
import de.hpi.accidit.model.VarDescriptor;

/**
 *
 * @author Arian Treffer
 */
public class VariableTrace {
    
    private final VarDescriptor var;
    private final long step;
    private final PrimitiveType primType;
    private final long value;

    public VariableTrace(VarDescriptor var, long step, PrimitiveType primType, long value) {
        this.var = var;
        var.ensurePersisted();
        this.step = step;
        this.primType = primType;
        this.value = value;
    }
    
}
