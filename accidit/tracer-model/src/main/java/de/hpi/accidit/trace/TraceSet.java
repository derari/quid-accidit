package de.hpi.accidit.trace;

import de.hpi.accidit.model.Model;

/**
 *
 * @author Arian Treffer
 */
public class TraceSet {
    
    private final Model model;

    public TraceSet(Model model) {
        this.model = model;
    }
    
    public ThreadTrace begin(int methodCode) {
        return new ThreadTrace(model);
    }

    public Model getModel() {
        return model;
    }
    
}
