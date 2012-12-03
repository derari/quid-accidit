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
    private final TypeDescriptor type;
    private final int codeId;
    private int modelId = -1;
    
    public FieldDescriptor(Model model, TypeDescriptor owner, String name, TypeDescriptor type, int codeId) {
        this.model = model;
        this.owner = owner;
        this.name = name;
        this.type = type;
        this.codeId = codeId;
    }
    
    public int getId() {
        return modelId;
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

    public TypeDescriptor getType() {
        return type;
    }
    
    public void ensurePersisted() {
        if (modelId >= 0) return;
        synchronized (model) {
            if (modelId >= 0) return;
            owner.ensurePersisted();
            type.ensurePersisted();
            modelId = model.nextFieldId();
            model.out.field(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("%05d)(%s#%s (%s", codeId, getOwner(), getName(), getType());
    }

}
