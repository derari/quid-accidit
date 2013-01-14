package de.hpi.accidit.trace;

import de.hpi.accidit.model.PrimitiveType;

/**
 * A method exit, caused by return or throw.
 * 
 * Stores the return value or the exception thrown
 * 
 * @author Arian Treffer
 */
public class ExitTrace extends ValueTrace {
    
    private long entry;
    private boolean returned;

    public ExitTrace(long entry, boolean returned, int line, long step, PrimitiveType primType, long valueId) {
        super(line, step, primType, valueId);
        this.entry = entry;
        this.returned = returned;
    }

    public long getEntry() {
        return entry;
    }

    public boolean isReturned() {
        return returned;
    }
            
    @Override
    public String toString() {
        return String.format("%s %s", 
                returned ? "RETURN" : "THROW", getPrimType().toString(getValueId()));
    }
    
}
