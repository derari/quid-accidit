package de.hpi.accidit.model;

public enum State {
    
    NEW,
    INITIALIZING,
    INITIALIZED,
    PERSISTING,
    PERSISTED;
    
    public boolean isInitializing() {
        return ordinal() >= INITIALIZING.ordinal();
    }
    
    public boolean isInitialized() {
        return ordinal() >= INITIALIZED.ordinal();
    }
    
    public boolean isPersisting() {
        return ordinal() >= PERSISTING.ordinal();
    }
    
    public boolean isPersisted() {
        return ordinal() >= PERSISTED.ordinal();
    }

    public State makeInitialized() {
        if (isInitialized()) throw new IllegalStateException("Is already " + this);
        return INITIALIZED;
    }
    
    public State makePersisted() {
        if (isPersisted()) throw new IllegalStateException("Is already " + this);
        return PERSISTED;
    }
    
}
