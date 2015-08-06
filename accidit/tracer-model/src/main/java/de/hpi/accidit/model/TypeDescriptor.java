package de.hpi.accidit.model;

import java.lang.reflect.Array;
import java.util.*;

/**
 *
 * @author Arian Treffer
 */
public class TypeDescriptor {

    public static String className(Class clazz) {
        String name = clazz.getName();
        if (name.startsWith("[")) name = descriptorToName(name);
        return name;
    }
    
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
    final ClassLoader cl;
    private final String name;
    private final int codeId;
    private final List<TypeDescriptor> supers = new ArrayList<>();
    private final Map<String, MethodDescriptor> methodBySig = new HashMap<>();
    private final Map<String, FieldDescriptor> fieldByName = new HashMap<>();
    private final PrimitiveType primType;
    private TypeDescriptor component = null;
    private String source = null;
    
    private int modelId = -1;
    private State state = State.NEW;

    public TypeDescriptor(ClassLoader cl, Model model, String name, int codeId) {
        this.cl = cl;
        this.model = model;
        this.name = name;
        this.codeId = codeId;
        this.primType = PrimitiveType.forClass(name);
    }

    public int getModelId() {
        return modelId;
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

    public String getSource() {
        return source;
    }

    public List<TypeDescriptor> getSupers() {
        return supers;
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

    public void setSource(String source) {
        this.source = source;
    }
    
    public void addSuper(String superName) {
        if (state.isInitialized()) 
            throw new IllegalStateException(
                "Supers of " + this + " already initialized");
        supers.add(model.getType(superName, cl));
    }

    public void ensureInitialized() {
        if (state.isInitialized()) return;
        synchronized (model) {
            if (state.isInitializing()) return;
            try {
                init();
            } catch (Exception e) {
                throw new RuntimeException(name, e);
            }
        }
    }

    private void init() throws ClassNotFoundException {
        state = State.INITIALIZING;
        try {
            Class clazz = getClass(cl, name);
            if (state.isInitialized()) {
                // loading with `getClass` might trigger initialization
                return;
            }

            Class sup = clazz.getSuperclass();
            if (sup != null) addSuper(className(sup));
            for (Class iface: clazz.getInterfaces()) {
                addSuper(className(iface));
            }

            if (clazz.isArray()) {
                Class comp = clazz.getComponentType();
                component = model.getType(className(comp), cl);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + name);
        }
        
        initCompleted();
    }
    
    public void initCompleted() {
        state = state.makeInitialized();
    }
    
    public void ensurePersisted() {
        if (state.isPersisted()) return;
        synchronized (model) {
            if (state.isPersisting()) return;
            ensureInitialized();
            state = state.makePersisted();
            if (component != null) component.ensurePersisted();
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

    private static Class<?> getClass(ClassLoader cl, String name) throws ClassNotFoundException {
        if (name.endsWith("[]")) {
            return getArrayClass(cl, name);
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
            default: return Class.forName(name, false, cl);
        }
    }

    private static Class<?> getArrayClass(ClassLoader cl, String name) throws ClassNotFoundException {
        int i = name.indexOf('[');
        Class comp = getClass(cl, name.substring(0, i));
        int dim = 0;
        while (i > -1) {
            dim++;
            i = name.indexOf('[', i+1);
        }
        return Array.newInstance(comp, new int[dim]).getClass();
    }

    public boolean isInitialized() {
        return state.isInitialized();
    }
    
}
