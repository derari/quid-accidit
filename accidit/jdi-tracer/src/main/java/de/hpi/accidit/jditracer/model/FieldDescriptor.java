package de.hpi.accidit.jditracer.model;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.VMDisconnectedException;

/**
 *
 * @author Arian Treffer
 */
public class FieldDescriptor extends LazyDescriptor<Field> {
    
    private final int id;
    private TypeDescriptor declaringType;
    //private TypeDescriptor type = null;
    private String name;
    
    public FieldDescriptor(int id) {
        this.id = id;
    }
    
    void initialize(Model model, TypeDescriptor declaringType, Field field) {
        this.declaringType = declaringType;
        this.name = field.name();
        initializeLazy(model, field);
    }

    @Override
    protected boolean init(Model model, Field field) {
//        try {
//            type =  model.getType(field.type());
//        } catch (ClassNotLoadedException | VMDisconnectedException ex) {
//            type = model.getType(field.typeName());
//            if (type == null) return false;
//        }
        model.out.field(this);
        return true;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TypeDescriptor getDeclaringType() {
        return declaringType;
    }

//    public TypeDescriptor getType() {
//        return type;
//    }

    @Override
    public String toString() {
        return declaringType.getName() + "#" + name + " (" + id + /*"/" + type + */")";
    }

}
