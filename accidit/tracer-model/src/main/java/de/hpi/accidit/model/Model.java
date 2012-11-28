package de.hpi.accidit.model;

import de.hpi.accidit.out.Out;
import java.util.*;

/**
 *
 * @author Arian Treffer
 */
public class Model {
    
    private final Map<String, TypeDescriptor> typeByName = new HashMap<>();
    private final List<TypeDescriptor> typeByCodeId = new ArrayList<>();
    private final List<MethodDescriptor> methodByCodeId = new ArrayList<>();
    private final List<FieldDescriptor> fieldByCodeId = new ArrayList<>();
    
    private int nextTypeModelId = 0;
    private int nextMethodModelId = 0;
    
    public final Out out;

    public Model(Out out) {
        this.out = out;
    }

    public TypeDescriptor getType(int codeId) {
        return typeByCodeId.get(codeId);
    }
    
    public MethodDescriptor getMethod(int codeId) {
        return methodByCodeId.get(codeId);
    }

    public FieldDescriptor getField(int codeId) {
        return fieldByCodeId.get(codeId);
    }
    
    public synchronized TypeDescriptor getType(String name) {
        TypeDescriptor t = typeByName.get(name);
        if (t == null) {
            t = new TypeDescriptor(this, name, typeByCodeId.size());
            typeByName.put(name, t);
            typeByCodeId.add(t);
        }
        return t;
    }

    synchronized MethodDescriptor createMethod(TypeDescriptor type, String name, String desc) {
        MethodDescriptor m = new MethodDescriptor(this, type, name, desc, methodByCodeId.size());
        methodByCodeId.add(m);
        return m;
    }

    synchronized FieldDescriptor createField(TypeDescriptor type, String name, String desc) {
        FieldDescriptor f = new FieldDescriptor(this, type, name, desc, fieldByCodeId.size());
        fieldByCodeId.add(f);
        return f;
    }

    int nextTypeId() {
        return nextTypeModelId++;
    }
    
    int nextMethodId() {
        return nextMethodModelId++;
    }
    
}
