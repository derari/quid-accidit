package de.hpi.accidit.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Arian Treffer
 */
public class MethodDescriptor {
    
    private final Model model;
    private final TypeDescriptor owner;
    private final String name;
    private final String desc;
    private final int codeId;
    private final List<VarDescriptor> variables = new ArrayList<>(5);
    private final PrimitiveType resultType;
    
    private int modelId = -1;
    private boolean varsInitialized = false;
    private boolean persisted = false;

    public MethodDescriptor(Model model, TypeDescriptor owner, String name, String desc, int codeId) {
        this.model = model;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.codeId = codeId;
        this.resultType = PrimitiveType.resultOfMethod(desc);
    }

    public int getCodeId() {
        return codeId;
    }

    public Model getModel() {
        return model;
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

    public PrimitiveType getResultType() {
        return resultType;
    }

    public void addVariable(int id, String name, String type) {
        if (varsInitialized) throw new IllegalStateException("Variables already initialized");
        while (id >= variables.size()) variables.add(null);
        TypeDescriptor t = model.getType(type);
        VarDescriptor var = new VarDescriptor(id, name, this, t);
        variables.set(id, var);
    }
    
    public VarDescriptor getVariable(int id) {
        return variables.get(id);
    }
    
    public void variablesCompleted() {
        if (varsInitialized) throw new IllegalStateException("Variables already initialized");
        varsInitialized = true;
    }

    public void ensurePersisted() {
        if (persisted) return;
        synchronized (model) {
            if (persisted) return;
            persisted = true;
            owner.ensurePersisted();
            modelId = model.nextMethodId();
            model.out.method(this);
        }
    }

    @Override
    public String toString() {
        if (modelId < 0) {
            return String.format("%05d)(%s#%s%s", codeId, getOwner(), getName(), getDescriptor());
        } else {
            return String.format("%05d/%05d)(%s#%s%s", codeId, modelId, getOwner(), getName(), getDescriptor());
        }
    }

}
