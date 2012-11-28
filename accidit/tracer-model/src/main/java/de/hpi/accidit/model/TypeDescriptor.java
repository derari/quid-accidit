package de.hpi.accidit.model;

import java.util.*;

/**
 *
 * @author Arian Treffer
 */
public class TypeDescriptor {
    
    final Model model;
    private final String name;
    private final int codeId;
    private final List<TypeDescriptor> supers = new ArrayList<>();
    private final Map<String, MethodDescriptor> methodBySig = new HashMap<>();
    private final Map<String, FieldDescriptor> fieldByName = new HashMap<>();
    
    private int modelId = -1;
    private boolean supersInitialized = false;
    private boolean persisted = false;

    public TypeDescriptor(Model model, String name, int codeId) {
        this.model = model;
        this.name = name;
        this.codeId = codeId;
    }
    
    public int getCodeId() {
        return codeId;
    }

    public String getName() {
        return name;
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
        if (supersInitialized) throw new IllegalStateException(
                "Supers of " + this + " already initialized");
        supers.add(model.getType(superName));
    }

    public void supersCompleted() {
        supersInitialized = true;
    }

    public void ensureSupersInitialized() {
        if (supersInitialized) return;
        synchronized (this) {
            if (supersInitialized) return;
            try {
                Class clazz = Class.forName(name);
                Class sup = clazz.getSuperclass();
                if (sup != null) addSuper(sup.getCanonicalName());
                for (Class iface: clazz.getInterfaces())
                    addSuper(iface.getCanonicalName());
                supersCompleted();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void ensurePersisted() {
        if (persisted) return;
        synchronized (this) {
            if (persisted) return;
            persisted = true;
            ensureSupersInitialized();
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

}
