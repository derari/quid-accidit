package de.hpi.accidit.db;

public interface Model {
    
    int lookupTypeId(int id);
    
    int lookupType(int id, String name);
    
}
