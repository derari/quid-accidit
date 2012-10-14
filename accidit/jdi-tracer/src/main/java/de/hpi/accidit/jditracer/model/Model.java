package de.hpi.accidit.jditracer.model;

import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;
import de.hpi.accidit.jditracer.out.Out;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Arian Treffer
 */
public class Model {
    
    private static final int CAP = 1024;
    
    final Out out;
    
    private final Map<String, TypeDescriptor> typeMap = new HashMap<>(CAP);
    private final Map<Method, MethodDescriptor> methodMap = new HashMap<>(CAP);
    private final Map<Field, FieldDescriptor> fieldMap = new HashMap<>(CAP);
    private final Map<String, SourceDescriptor> sourceMap = new HashMap<>(CAP);
    
    private int nextTypeId = 0;
    private int nextMethodId = 0;
    private int nextFieldId = 0;
    private int nextSourceId = 0;
    
    public Model(Out out) {
        this.out = out;
    }
    
    public TypeDescriptor getType(String name) {
        return typeMap.get(name);
    }
    
    public TypeDescriptor getType(Type type) {
        TypeDescriptor desc = typeMap.get(type.name());
        if (desc == null) {
            desc = new TypeDescriptor();
            typeMap.put(type.name(), desc);
            desc.initialize(this, type);
        }
        return desc;
    }
    
    TypeDescriptor createTypeDummy(String name, VirtualMachine vm) {
        TypeDescriptor desc = getType(name);
        if (desc == null) {
            desc = new TypeDescriptor(name, vm);
            typeMap.put(name, desc);
            desc.initialize(this, null);            
        }
        return desc;
    }
    
    int nextTypeId() {
        return nextTypeId++;
    }
    
    public MethodDescriptor getMethod(Method method) {
        MethodDescriptor desc = methodMap.get(method);
        if (desc == null) {
            return getType(method.declaringType()).getMethod(method);
        }
        return desc;
    }
    
    public FieldDescriptor getField(Field field) {
        FieldDescriptor desc = fieldMap.get(field);
        if (desc == null) {
            return getType(field.declaringType()).getField(field);
        }
        return desc;
    }

    MethodDescriptor createMethod(TypeDescriptor type, Method method) {
        MethodDescriptor desc = new MethodDescriptor(nextMethodId++);
        methodMap.put(method, desc);
        desc.initialize(this, type, method);
        return desc;
    }
    
    FieldDescriptor createField(TypeDescriptor type, Field field) {
        FieldDescriptor desc = new FieldDescriptor(nextFieldId++);
        fieldMap.put(field, desc);
        desc.initialize(this, type, field);
        return desc;
    }

//    public SourceDescriptor getSource(String path) {
//        SourceDescriptor desc = sourceMap.get(path);
//        if (desc == null) {
//            desc = new SourceDescriptor(nextSourceId++, path);
//            sourceMap.put(path, desc);
//            out.source(desc);
//        }
//        return desc;
//    }
}
