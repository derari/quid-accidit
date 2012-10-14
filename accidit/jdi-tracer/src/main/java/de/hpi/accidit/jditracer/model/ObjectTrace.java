package de.hpi.accidit.jditracer.model;

/**
 *
 * @author Arian Treffer
 */
public class ObjectTrace {
    
    private final int testId;
    private final TypeDescriptor type;
    private final long id;
    private Model model;

    public ObjectTrace(Model model, TypeDescriptor type, int testId, long objectId) {
        this.type = type;
        this.id = objectId;
        this.testId = testId;
        this.model = model;
    }

    public long getId() {
        return id;
    }

    public int getTestId() {
        return testId;
    }

    public TypeDescriptor getType() {
        return type;
    }
    
    public void ensureIsTraced() {
        if (model != null) {
            model.out.object(this);
            model = null;
        }
    }

    @Override
    public String toString() {
        return type.getName() + "_" + id;
    }
    
}
