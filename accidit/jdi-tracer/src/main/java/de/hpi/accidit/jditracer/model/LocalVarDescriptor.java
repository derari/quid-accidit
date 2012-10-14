package de.hpi.accidit.jditracer.model;

import com.sun.jdi.*;

/**
 *
 * @author Arian Treffer
 */
public class LocalVarDescriptor extends LazyDescriptor<LocalVariable> {
    
    private final MethodDescriptor method;
    private final int id;
    private final String name;
    private final boolean arg;
    private TypeDescriptor type;
    
    public LocalVarDescriptor(MethodDescriptor method, int vid, LocalVariable lvar, Model model) {
        this.method = method;
        this.id = vid;
        this.name = lvar.name();
        this.arg = lvar.isArgument();
        initializeLazy(model, lvar);
    }
    
    @Override
    protected boolean init(Model model, LocalVariable var) {
        try {
            type = model.getType(var.type());
        } catch (ClassNotLoadedException | VMDisconnectedException ex) {
            type = model.getType(var.typeName());
            if (type == null) return false;
        }
        model.out.local(this);
        return true;
    }

    public MethodDescriptor getMethod() {
        return method;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isArg() {
        return arg;
    }

    public TypeDescriptor getType() {
        return type;
    }
    
//    static void saveLocals(File file, Collection<MethodDescriptor> values, int objectTypeId) throws FileNotFoundException {
//        try (PrintWriter w = new PrintWriter(file)) {
//            for (MethodDescriptor m: values) {
//                for (LocalVarDescriptor l: m.getLocals()) {
//                    w.print(m.getId());
//                    w.print(";");
//                    w.print(l.id);
//                    w.print(";");
//                    w.print(l.name);
//                    w.print(";");
//                    w.print(l.arg ? 1 : 0);
//                    w.print(";");
//                    TypeDescriptor t = l.getType();
//                    w.print(t != null ? t.getId() : objectTypeId);
//                    w.println();
//                }
//            }
//        }
//    }

    @Override
    public String toString() {
        return method + " " + (arg? ">" : "") + name + " (" + id + "/" + type + ")";
    }

}
