package de.hpi.accidit.jditracer.model;

/**
 *
 * @author Arian Treffer
 */
public abstract class LazyDescriptor<Mirror> {
    
    private Model model = null;
    private Mirror mirror = null;
    
    protected final void initialize(Model model, Mirror mirror) {
//        preInit(model, mirror);
//        if (!init(model, mirror)) {
//            initializeLazy(model, mirror);
//        }
        initializeLazy(model, mirror);
    }
    
    protected final void initializeLazy(Model model, Mirror mirror) {
        preInit(model, mirror);
        this.model = model;
        this.mirror = mirror;
    }
    
    protected final void requireInitialized() {
        if (!tryInitialize()) {
            throw new IllegalStateException("Not initialized");
        }
    }
    
    protected final boolean tryInitialize() {
        if (model == null) return true;
        if (init(model, mirror)) {
            model = null;
            mirror = null;
            return true;
        }
        return false;
    }

    protected void preInit(Model model, Mirror mirror) {
    }
    
    protected abstract boolean init(Model model, Mirror mirror);
    
}
