package de.hpi.accidit.jditracer.model;

/**
 *
 * @author Arian Treffer
 */
public class SourceDescriptor {
    
    private final int id;
    private final String file;

    public SourceDescriptor(int id, String file) {
        this.id = id;
        this.file = file;
    }

    public int getId() {
        return id;
    }

    public String getFile() {
        return file;
    }
    
}
