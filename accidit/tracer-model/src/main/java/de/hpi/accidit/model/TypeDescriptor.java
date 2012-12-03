package de.hpi.accidit.model;

import java.lang.reflect.Array;
import java.util.*;

/**
 *
 * @author Arian Treffer
 */
public class TypeDescriptor {
    
    public static String descriptorToName(String desc) {
        int dim = desc.lastIndexOf('[')+1;
        desc = desc.substring(dim).replace('/', '.');
        PrimitiveType pt = PrimitiveType.forDescriptor(desc);
        if (pt != PrimitiveType.OBJECT) return arrayTypeName(pt.toString().toLowerCase(), dim);
        return arrayTypeName(desc.substring(1, desc.length()-1), dim);
    }
    
    private static String arrayTypeName(String name, int dim) {
        if (dim == 0) return name;
        StringBuilder sb = new StringBuilder(name.length() + dim*2);
        sb.append(name);
        for (int i = 0; i < dim; i++)
            sb.append("[]");
        return sb.toString();
    }

    final Model model;
    private final String name;
    private final int codeId;
    private final List<TypeDescriptor> supers = new ArrayList<>();
    private final Map<String, MethodDescriptor> methodBySig = new HashMap<>();
    private final Map<String, FieldDescriptor> fieldByName = new HashMap<>();
    private final PrimitiveType primType;
    private TypeDescriptor component = null;
    
    private int modelId = -1;
    private boolean initialized = false;
    private boolean persisted = false;

    public TypeDescriptor(Model model, String name, int codeId) {
        this.model = model;
        this.name = name;
        this.codeId = codeId;
        this.primType = PrimitiveType.forClass(name);
    }
    
    public int getCodeId() {
        return codeId;
    }

    public String getName() {
        return name;
    }

    public TypeDescriptor getComponentType() {
        return component;
    }

    public synchronized MethodDescriptor getMethod(String name, String desc) {
        String sig = name + "#" + desc;
        MethodDescriptor m = methodBySig.get(sig);
        if (m == null) {
            m = model.createMethod(this, name, desc);
            methodBySig.put(sig, m);
        }
        return m;
    }

    public FieldDescriptor getField(String name, String desc) {
        FieldDescriptor f = fieldByName.get(name);
        if (f == null) {
            f = model.createField(this, name, desc);
            fieldByName.put(name, f);
        }
        return f;
    }
    
    public void addSuper(String superName) {
        if (initialized) throw new IllegalStateException(
                "Supers of " + this + " already initialized");
        supers.add(model.getType(superName));
    }

    public void ensureInitialized() {
        if (initialized) return;
        synchronized (model) {
            if (initialized) return;
            try {
                init();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void init() throws ClassNotFoundException {
        Class clazz = getClass(name);
        
        Class sup = clazz.getSuperclass();
        if (sup != null) addSuper(sup.getCanonicalName());
        for (Class iface: clazz.getInterfaces())
            addSuper(iface.getCanonicalName());
        
        if (clazz.isArray()) {
            Class comp = clazz.getComponentType();
            component = model.getType(comp.getSimpleName());
        }
        
        initCompleted();
    }
    
    public void initCompleted() {
        initialized = true;
    }
    
    public void ensurePersisted() {
        if (persisted) return;
        synchronized (model) {
            if (persisted) return;
            persisted = true;
            ensureInitialized();
            for (TypeDescriptor td: supers)
                td.ensurePersisted();
            modelId = model.nextTypeId();
            model.out.type(this);
        }
    }

    @Override
    public String toString() {
        if (modelId < 0) {
            return String.format("%04d)%s", codeId, getName());
        } else {
            return String.format("%04d/%04d)%s", codeId, modelId, getName());
        }
    }    

    public PrimitiveType getPrimitiveType() {
        return primType;
    }

    private static Class<?> getClass(String name) throws ClassNotFoundException {
        if (name.endsWith("[]")) {
            return getArrayClass(name);
        }
        switch (name) {
            case "boolean": return boolean.class;
            case "byte": return byte.class;
            case "char": return char.class;
            case "double": return double.class;
            case "float": return float.class;
            case "int": return int.class;
            case "long": return long.class;
            case "short": return short.class;
            case "void": return void.class;
            default: return Class.forName(name);
        }
    }

    private static Class<?> getArrayClass(String name) throws ClassNotFoundException {
        int i = name.indexOf('[');
        Class comp = getClass(name.substring(0, i));
        int dim = 0;
        while (i > -1) {
            dim++;
            i = name.indexOf('[', i+1);
        }
        return Array.newInstance(comp, new int[dim]).getClass();
    }
    
}
