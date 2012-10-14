package de.hpi.accidit.jditracer.model;

import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

/**
 *
 * @author Arian Treffer
 */
public class NullValue implements Value {

    public static final Value NULL = new NullValue();
    
    @Override
    public Type type() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VirtualMachine virtualMachine() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String toString() {
        return "null";
    }
    
}
