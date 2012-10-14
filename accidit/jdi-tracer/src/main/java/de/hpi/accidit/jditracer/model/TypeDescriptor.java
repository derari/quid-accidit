package de.hpi.accidit.jditracer.model;

import com.sun.jdi.*;
import java.util.*;

/**
 * A type in the target VM.
 * 
 * <p/>
 * Is immediately written to output.
 * Methods, fields and supertypes are available after initialization.
 * 
 * @author Arian Treffer
 */
public class TypeDescriptor extends LazyDescriptor<Type> {
    
    static final String[] IGNORE_INTERNALS = {
        "java.",
        "sun.",
    };

    private int id;
    private String name;
    private String file = null;
    private boolean ignoreInternals;
    private final Set<TypeDescriptor> supers = new HashSet<>(6);
    private final Map<Method, MethodDescriptor> methods = new HashMap<>();
    private final Map<Field, FieldDescriptor> fields = new HashMap<>();
    
    private VirtualMachine vm = null;

    public TypeDescriptor() {
    }

    public TypeDescriptor(String dummyName, VirtualMachine vm) {
        this.name = dummyName;
        this.vm = vm;
    }

    @Override
    protected void preInit(Model model, Type type) {
        this.id = model.nextTypeId();
        if (name == null) {
            name = type.name();
            if (type instanceof ReferenceType) {
                try {
                    List<String> sources = ((ReferenceType) type).sourcePaths(type.virtualMachine().getDefaultStratum());
                    if (!sources.isEmpty()) file = sources.get(0);
                } catch (AbsentInformationException ex) {
                }            
            }
        } else { System.err.println("No source for " + name); }
        for (String i: IGNORE_INTERNALS) {
            if (name.startsWith(i)) {
                ignoreInternals = true;
                break;
            }
        }
        model.out.type(this);
    }
    
    @Override
    protected boolean init(Model model, Type type) {
        if (type == null && vm != null) {
            List<ReferenceType> types = vm.classesByName(name);
            for (Type t: types) {
                if (init(model, t)) return true;
            }
            return false;
        }
        
        try {
            if (type instanceof ClassType) {
                ClassType ctype = (ClassType) type;
                Type sup = ctype.superclass();
                if (sup != null) supers.add(model.getType(sup));
                for (InterfaceType i: ctype.interfaces()) {
                    supers.add(model.getType(i));
                }
            }
            if (type instanceof InterfaceType) {
                InterfaceType itype = (InterfaceType) type;
                for (InterfaceType i: itype.superinterfaces()) {
                    supers.add(model.getType(i));
                }
            }
        } catch (ClassNotPreparedException | VMDisconnectedException e) {
            return false;
        }
        model.out.supers(this);
        if (type instanceof ReferenceType) {
            ReferenceType rtype = (ReferenceType) type;
            for (Method m: rtype.methods()) {
                methods.put(m, model.createMethod(this, m));
            }
            for (Field f: rtype.fields()) {
                fields.put(f, model.createField(this, f));
            }
        }
        return true;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFile() {
        return file;
    }
    
    public boolean ignoreInternals() {
        return ignoreInternals;
    }

    public Set<TypeDescriptor> getSupers() {
        return supers;
    }

    public MethodDescriptor getMethod(Method m) {
        requireInitialized();
        return methods.get(m);
    }

    public FieldDescriptor getField(Field field) {
        requireInitialized();
        return fields.get(field);
    }

    @Override
    public String toString() {
          return name + " (" + id + "/" + methods.size() + "/" + fields.size() + ")";
    }
    
}
