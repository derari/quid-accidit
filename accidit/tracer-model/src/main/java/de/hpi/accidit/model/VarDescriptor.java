package de.hpi.accidit.model;

/**
 *
 * @author Arian Treffer
 */
public class VarDescriptor {
    
    private final int id;
    private final String name;
    private final MethodDescriptor method;
    private final TypeDescriptor type;
    private boolean persisted = false;

    public VarDescriptor(int id, String name, MethodDescriptor method, TypeDescriptor type) {
        this.id = id;
        this.name = name;
        this.method = method;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public MethodDescriptor getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }

    public TypeDescriptor getType() {
        return type;
    }

    public PrimitiveType getPrimitiveType() {
        return type.getPrimitiveType();
    }
    
    public void ensurePersisted() {
        if (persisted) return;
        synchronized (type.model) {
            if (persisted) return;
            persisted = true;
            method.ensurePersisted();
            type.ensurePersisted();
            type.model.out.variable(this);
        }
    }

    @Override
    public String toString() {
        return String.format("%03d (%s %s", id, type, name);
    }    
    
}
