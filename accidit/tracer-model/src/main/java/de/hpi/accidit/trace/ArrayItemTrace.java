package de.hpi.accidit.trace;

import de.hpi.accidit.model.FieldDescriptor;
import de.hpi.accidit.model.PrimitiveType;
import de.hpi.accidit.model.TypeDescriptor;
import de.hpi.accidit.model.VarDescriptor;

/**
 * Traces field reads and writes.
 * 
 * @author Arian Treffer
 */
public class ArrayItemTrace extends ValueTrace {
    
    private final ObjectTrace instance;
    private final int index;

    public ArrayItemTrace(ObjectTrace instance, int index, int line, long step, PrimitiveType primType, long valueId) {
        super(line, step, primType, valueId);
        this.instance = instance;
        this.index = index;
    }

    @Override
    public String toString() {
        return String.format("%03d:%s[%d] == %s", 
                getLine(), instance, index, getPrimType().toString(getValueId()));
    }
    
}
