package de.hpi.accidit.jditracer.model;

/**
 *
 * @author Arian Treffer
 */
public class LocalVarTrace extends ValueTrace {

    private final LocalVarDescriptor lvar;

    public LocalVarTrace(LocalVarDescriptor lvar) {
        this.lvar = lvar;
        lvar.requireInitialized();
//        lvar.getType().requireInitialized();
    }

    public LocalVarDescriptor getVariable() {
        return lvar;
    }
    
}
