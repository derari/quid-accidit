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
    private int line = -1;
    
    private int nextVarId = 0;
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

    public int getModelId() {
        return modelId;
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

    public void setLine(int line) {
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public VarDescriptor addVariable(int index, int startOffset, String name, String type) {
        if (varsInitialized) throw new IllegalStateException("Variables already initialized");
        TypeDescriptor t = model.getType(type, owner.cl);
        int id = nextVarId++;
        VarDescriptor var = new VarDescriptor(id, index, startOffset, name, this, t);
        for (int i = index-variables.size(); i >= 0; i--) variables.add(null);
        VarDescriptor old = variables.get(index);
        if (old == null) {
            variables.set(index, var);
        } else {
            variables.set(index, old.insert(var));
        }
        return var;
    }
    
    private boolean varsWarned = false;
    
    public VarDescriptor getVariable(int id, int offset) {
        if (id >= variables.size()) {
//            if (!varsWarned) {
//                System.out.println(this + ": " + variables.size() + " variables");
//                for (VarDescriptor v: variables) {
//                    System.out.println("  " + v);
//                }
//                varsWarned = true;
//            }
            return null;
        }
        return variables.get(id).get(offset);
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

    public boolean variablesAreInitialized() {
        return varsInitialized;
    }

    public boolean implementsMethod(int codeId) {
        if (codeId == -1) return false;
        if (codeId == this.codeId) return true;
        MethodDescriptor m = model.getMethod(codeId);
        // this is an approximation...
        return m.getName().equals(name);
    }

}
