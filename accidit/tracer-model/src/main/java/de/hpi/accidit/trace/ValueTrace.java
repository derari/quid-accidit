package de.hpi.accidit.trace;

import de.hpi.accidit.model.PrimitiveType;

/**
 * Traces a value that occoured at some point.
 */
public abstract class ValueTrace {
    
    private final int line;
    private final long step;
    private final PrimitiveType primType;
    private final long valueId;

    public ValueTrace(int line, long step, PrimitiveType primType, long valueId) {
        this.line = line;
        this.step = step;
        this.primType = primType;
        this.valueId = valueId;
    }

    public int getLine() {
        return line;
    }

    public long getStep() {
        return step;
    }

    public PrimitiveType getPrimType() {
        return primType;
    }

    public long getValueId() {
        return valueId;
    }
    
}
