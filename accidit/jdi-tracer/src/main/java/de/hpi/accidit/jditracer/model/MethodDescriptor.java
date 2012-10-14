package de.hpi.accidit.jditracer.model;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import java.util.*;

/**
 * A method of a type.
 * 
 * <p/>
 * Is immediately written to output.
 * Local variables are available after initialization.
 * 
 * @author Arian Treffer
 */
public class MethodDescriptor extends LazyDescriptor<Method> {

    private final int id;
    private TypeDescriptor declaringType;
    private String name;
    private String signature;
    
    private final Map<String, LocalVarDescriptor> locals = new HashMap<>(16);

    public MethodDescriptor(int id) {
        this.id = id;
    }
    
    void initialize(Model model, TypeDescriptor declaringType, Method method) {
        this.declaringType = declaringType;
        this.signature = method.signature();
        this.name = method.declaringType().name() + "#" + method.name();
        model.out.method(this);
        initializeLazy(model, method);
    }

    @Override
    protected boolean init(Model model, Method method) {
        int lvarId = 0;
        try {
            for (LocalVariable lvar: method.variables()) {
                LocalVarDescriptor lv = new LocalVarDescriptor(this, lvarId++, lvar, model);
                locals.put(lvar.name(), lv);
            }
        } catch (AbsentInformationException e) { }   
        return true;
    }
    
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public TypeDescriptor getDeclaringType() {
        return declaringType;
    }
    
    public boolean ignoreInternals() {
        return declaringType.ignoreInternals();
    }

    public LocalVarDescriptor getLocal(LocalVariable lv) {
        LocalVarDescriptor d = locals.get(lv.name());
        if (d != null) return d;
        throw new IllegalArgumentException(this + " " + lv.name());
    }

    @Override
    public String toString() {
        return declaringType.getName() + "#" + name + " (" + id + "/" + locals.size() + ")";
    }

}
