package de.hpi.accidit.model;

/**
 *
 * @author Arian Treffer
 */
public class VarDescriptor {
    
    private final int id;
    private final int index;
    private final int offset;
    private final String name;
    private final MethodDescriptor method;
    private final TypeDescriptor type;
    private boolean argument = false;
    private boolean persisted = false;
    
    private VarDescriptor next;

    public VarDescriptor(int id, int index, int offset, String name, MethodDescriptor method, TypeDescriptor type) {
        this.id = id;
        this.index = index;
        this.offset = offset;
        this.name = name;
        this.method = method;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public int getIndex() {
        return index;
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

    public void setArgument(boolean argument) {
        this.argument = argument;
    }

    public boolean isArgument() {
        return argument;
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
    
    public VarDescriptor insert(VarDescriptor vd) {
        if (vd.offset < offset) {
            vd.next = this;
            return vd;
        }
        VarDescriptor last = this;
        VarDescriptor next = last.next;
        while (next != null && next.offset < vd.offset) {
            last = next;
            next = last.next;
        }
        vd.next = last.next;
        last.next = vd;
        return this;
    }

    public VarDescriptor get(int offset) {
        if (offset < 1) return this;
        VarDescriptor v = this;
        while (v != null && v.offset < offset) v = v.next;
        return v;
    }
    
    @Override
    public String toString() {
        return String.format("%03d (%s %s %03d %03d", id, type, name, index, offset);
    }    
    
}
