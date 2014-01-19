package de.hpi.accidit.db;

public class EmptyModel implements Model {

    @Override
    public int lookupTypeId(int id) {
        return id;
    }   

    @Override
    public int lookupType(int id, String name) {
        return id;
    }
}
