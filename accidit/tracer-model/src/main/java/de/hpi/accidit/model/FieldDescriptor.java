package de.hpi.accidit.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Arian Treffer
 */
public class FieldDescriptor {
    
    private final Model model;
    private final TypeDescriptor owner;
    private final String name;
    private final String desc;
    private final int codeId;
    
    public FieldDescriptor(Model model, TypeDescriptor owner, String name, String desc, int codeId) {
        this.model = model;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.codeId = codeId;
    }

    public int getCodeId() {
        return codeId;
    }

    public TypeDescriptor getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return desc;
    }

    @Override
    public String toString() {
        return String.format("%05d)(%s#%s%s", codeId, getOwner(), getName(), getDescriptor());
    }

}
