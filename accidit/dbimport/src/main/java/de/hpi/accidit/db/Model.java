package de.hpi.accidit.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Model implements AutoCloseable {
    
    private final Database db;
    private final PreparedStatement psFindType;

    public Model(Database db) throws SQLException {
        this.db = db;
        psFindType = db.prepare("");
    }
    
    public void addType(String[] row) {
        
    }
    
    
    @Override
    public void close() throws SQLException {
        
    }
    
    private static final int TYPE_ID = Database.fieldIndex("Type", "id");
    private static final int TYPE_NAME = Database.fieldIndex("Type", "name");

}
