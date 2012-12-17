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
public class FieldTrace extends ValueTrace {
    
    private final ObjectTrace instance;
    private final FieldDescriptor field;

    public FieldTrace(ObjectTrace instance, FieldDescriptor field, int line, long step, PrimitiveType primType, long valueId) {
        super(line, step, primType, valueId);
        this.instance = instance;
        this.field = field;
        field.ensurePersisted();
    }

    public ObjectTrace getInstance() {
        return instance;
    }

    public FieldDescriptor getField() {
        return field;
    }

    @Override
    public String toString() {
        if (instance == null) {
            return String.format("%03d:%s == %s", 
                    getLine(), field, getPrimType().toString(getValueId()));
        } else {
            return String.format("%03d:%s.%s == %s", 
                    getLine(), instance, field, getPrimType().toString(getValueId()));
        }
    }
    
}
