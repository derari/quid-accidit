package de.hpi.accidit.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Model implements AutoCloseable {
    
    private final Database db;
    private final Database.BulkImport bulkImport;
    private final PreparedStatement psFindType;
    private final List<Integer> typeMap = new ArrayList<>(1024);
    private int nextTypeId;

    public Model(Database db) throws SQLException {
        this.db = db;
        bulkImport = db.bulkImport();
        psFindType = db.prepare("SELECT `id` FROM `Type` WHERE `name` = ?");
        nextTypeId = db.getMaxId("Type") + 1;
    }
    
    public void beginTypes() throws SQLException {
        bulkImport.setTable("Type");
    }
    
    public void addType(String[] row) throws SQLException {
        int id = Integer.parseInt(row[TYPE_ID]);
        String name = row[TYPE_NAME];
        psFindType.setString(1, name);
        ResultSet rs = psFindType.executeQuery();
        if (rs.next()) {
            int dbId = rs.getInt(1);
            mapType(id, dbId);
        } else {
            int dbId = nextTypeId++;
            mapType(id, dbId);
            row[TYPE_ID] = String.valueOf(dbId);
            bulkImport.addRow(row);
        }
    }
    
    private void mapType(int dataId, int dbId) {
        int diff = typeMap.size() - dataId + 1;
        for (int i = 0; i < diff; i++) {
            typeMap.add(null);
        }
        typeMap.set(dataId, dbId);
    }
    
    public void beginMethods() throws SQLException {
        bulkImport.setTable("Method");
    }
    
    @Override
    public void close() throws SQLException {
        bulkImport.close();
        psFindType.close();
    }
    
    private static final int TYPE_ID = Database.fieldIndex("Type", "id");
    private static final int TYPE_NAME = Database.fieldIndex("Type", "name");

}
