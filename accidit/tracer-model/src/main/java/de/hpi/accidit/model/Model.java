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
    
//    private final MethodDescriptor nullMethod;
    
    private int nextTypeModelId = 0;
    private int nextMethodModelId = 0;
    private int nextFieldModelId = 0;
    
    public final Out out;

    public Model(Out out) {
        this.out = out;
//        nullMethod = new MethodDescriptor(this, getType("java.lang.Object"), "unknown", "()V", -1){
//            { variablesCompleted(); }
//        };
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
    
    public synchronized TypeDescriptor getType(String name, ClassLoader cl) {
        TypeDescriptor t = typeByName.get(name);
        if (t == null) {
            t = new TypeDescriptor(cl, this, name, typeByCodeId.size());
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

    synchronized FieldDescriptor createField(TypeDescriptor owner, String name, String desc) {
        String typeName = TypeDescriptor.descriptorToName(desc);
        TypeDescriptor type = getType(typeName, owner.cl);
        FieldDescriptor f = new FieldDescriptor(this, owner, name, type, fieldByCodeId.size());
        fieldByCodeId.add(f);
        return f;
    }

    int nextTypeId() {
        return nextTypeModelId++;
    }
    
    int nextMethodId() {
        return nextMethodModelId++;
    }

    int nextFieldId() {
        return nextFieldModelId++;
    }

//    public MethodDescriptor nullMethod() {
//        return nullMethod;
//    }
//    
}
