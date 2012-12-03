package de.hpi.accidit.trace;

import de.hpi.accidit.model.PrimitiveType;
import de.hpi.accidit.model.VarDescriptor;

/**
 *
 * @author Arian Treffer
 */
public class VariableTrace extends ValueTrace {
    
    private final VarDescriptor var;

    public VariableTrace(VarDescriptor var, long step, int line, PrimitiveType primType, long value) {
        super(line, step, primType, value);
        this.var = var;
        var.ensurePersisted();
    }

    @Override
    public String toString() {
        return String.format("%s <- %s%d", 
                var, getPrimType().getKey(), getValueId());
    }
    
}
