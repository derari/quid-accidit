package de.hpi.accidit.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Model implements AutoCloseable {
    
    private final Database db;
    private final Database.BulkImport bulkImport;
    private final PreparedStatement psFindType;
    private final PreparedStatement psMethodsForType;
    private final PreparedStatement psFieldsForType;
    private final List<Integer> typeMap = new ArrayList<>(1024);
    private final List<Integer> methodMap = new ArrayList<>(1024);
    private final List<Integer> fieldMap = new ArrayList<>(1024);
    private final Set<Integer> newMethods = new HashSet<>();
    private List<TypeData> typeDataMap = new ArrayList<>(1024);
    private int nextTypeId;
    private int nextMethodId;
    private int nextFieldId;
    private int traceIdOffset;

    public Model(Database db) throws SQLException {
        this.db = db;
        bulkImport = db.bulkImport();
        psFindType = db.prepare("SELECT `id` FROM `Type` WHERE `name` = ?");
        psMethodsForType = db.prepare("SELECT `id`, `name`, `signature`, `hashcode` FROM `Method` WHERE `declaringTypeId` = ?");
        psFieldsForType = db.prepare("SELECT `id`, `name` FROM `Field` WHERE `declaringTypeId` = ?");
        nextTypeId = db.getMaxId("Type") + 1;
        nextMethodId = db.getMaxId("Method") + 1;
        nextFieldId = db.getMaxId("Field") + 1;
        traceIdOffset = db.getMaxId("TestTrace") + 1;
        System.out.printf("#t: %d; #m: %d; #f: %d; T+=%d%n", nextTypeId, nextMethodId, nextFieldId, traceIdOffset);
    }
    
    public void beginTypes() throws SQLException {
        bulkImport.setTable("Type");
    }
    
    public void addType(String[] row) throws SQLException {
        int id = Integer.parseInt(row[TYPE_ID]);
        String name = row[TYPE_NAME];
        psFindType.setString(1, name);
        ResultSet rs = psFindType.executeQuery();
        int dbId;
        if (rs.next()) {
            dbId = rs.getInt(1);
        } else {
            dbId = nextTypeId++;
            row[TYPE_ID] = String.valueOf(dbId);
            bulkImport.addRow(row);
        }
        mapType(id, dbId);
        map(typeDataMap, id, new TypeData(dbId));
    }
    
    public void beginMethods() throws SQLException {
        bulkImport.setTable("Method");
    }
    
    public void addMethod(String[] row) {
        int id = Integer.parseInt(row[METHOD_ID]);
        int typeId = Integer.parseInt(row[METHOD_TYPE]);
        TypeData td = typeDataMap.get(typeId);
        String methodKey =
                row[METHOD_NAME] +
                row[METHOD_SIG] +
                Long.toHexString(Long.parseLong(row[METHOD_HASH]));
        int dbId = td.getMethodId(methodKey);
        if (dbId < 0) {
            newMethods.add(id);
            dbId = nextMethodId++;
            int dbTypeId = typeMap.get(typeId);
            row[METHOD_ID] = String.valueOf(dbId);
            row[METHOD_TYPE] = String.valueOf(dbTypeId);
            bulkImport.addRow(row);
        }
        mapMethod(id, dbId);
    }
    
    public void beginVariables() throws SQLException {
        bulkImport.setTable("Variable");
    }
    
    public void addVariable(String[] row) {
        int mId = Integer.parseInt(row[VARIABLE_METHOD]);
        if (newMethods.contains(mId)) {
            mId = methodMap.get(mId);
            row[VARIABLE_METHOD] = String.valueOf(mId);
            bulkImport.addRow(row);
        }
    }
    
    public void beginFields() throws SQLException {
        bulkImport.setTable("Field");
    }
    
    public void addField(String[] row) {
        int id = Integer.parseInt(row[FIELD_ID]);
        int typeId = Integer.parseInt(row[FIELD_TYPE]);
        TypeData td = typeDataMap.get(typeId);
        String fieldKey = row[FIELD_NAME];
        int dbId = td.getFieldId(fieldKey);
        if (dbId < 0) {
            dbId = nextFieldId++;
            int dbTypeId = typeMap.get(typeId);
            row[FIELD_ID] = String.valueOf(dbId);
            row[FIELD_TYPE] = String.valueOf(dbTypeId);
            bulkImport.addRow(row);
        }
        mapField(id, dbId);
    }
    
    public void beginTraces() throws SQLException {
        bulkImport.setTable("TestTrace");
    }
    
    public void addTrace(String[] row) {
        fixTraceId(row, TRACE_ID);
        bulkImport.addRow(row);
    }
    
    public void beginObjects() throws SQLException {
        bulkImport.setTable("ObjectTrace");
    }
    
    public void addObject(String[] row) {
        fixTraceId(row, OBJECT_TRACE);
        fixTypeId(row, OBJECT_TYPE);
        bulkImport.addRow(row);
    }
    
    public void beginCalls() throws SQLException {
        bulkImport.setTable("CallTrace");
    }
    
    public void addCall(String[] row) {
        fixTraceId(row, CALL_TRACE);
        fixMethodId(row, CALL_METHOD);
        bulkImport.addRow(row);
    }
    
    public void beginExits() throws SQLException {
        bulkImport.setTable("ExitTrace");
    }
    
    public void addExit(String[] row) {
        fixTraceId(row, EXIT_TRACE);
        bulkImport.addRow(row);
    }
    
    public void beginThrows() throws SQLException {
        bulkImport.setTable("ThrowTrace");
    }
    
    public void beginCatchs() throws SQLException {
        bulkImport.setTable("CatchTrace");
    }
    
    public void addException(String[] row) {
        fixTraceId(row, EXCEPTION_TRACE);
        bulkImport.addRow(row);
    }
    
    public void beginVariableSets() throws SQLException {
        bulkImport.setTable("VariableTrace");
    }
    
    public void addVariableSet(String[] row) {
        fixTraceId(row, VAR_SET_TRACE);
        fixMethodId(row, VAR_SET_METHOD);
        bulkImport.addRow(row);
    }
    
    public void beginFieldPuts() throws SQLException {
        bulkImport.setTable("PutTrace");
    }
    
    public void beginFieldGets() throws SQLException {
        bulkImport.setTable("GetTrace");
    }
    
    public void addFieldAccess(String[] row) {
        fixTraceId(row, FIELD_ACC_TRACE);
        fixFieldId(row, FIELD_ACC_FIELD);
        bulkImport.addRow(row);
    }
    
    public void beginArrayPuts() throws SQLException {
        bulkImport.setTable("ArrayPutTrace");
    }
    
    public void beginArrayGets() throws SQLException {
        bulkImport.setTable("ArrayGetTrace");
    }
    
    public void addArrayAccess(String[] row) {
        fixTraceId(row, FIELD_ACC_TRACE);
        bulkImport.addRow(row);
    }
    
    private void fixTypeId(String[] row, int index) {
        fixForeignKey(row, index, typeMap);
    }
    
    private void fixMethodId(String[] row, int index) {
        fixForeignKey(row, index, methodMap);
    }
    
    private void fixFieldId(String[] row, int index) {
        fixForeignKey(row, index, fieldMap);
    }
    
    private void fixForeignKey(String[] row, int index, List<Integer> map) {
        int id = Integer.parseInt(row[index]);
        row[index] = String.valueOf(map.get(id));
    }
    
    private void fixTraceId(String[] row, int index) {
        int id = Integer.parseInt(row[index]);
        row[index] = String.valueOf(id + traceIdOffset);
    }
    
    @Override
    public void close() throws SQLException {
        bulkImport.close();
        psFindType.close();
        psMethodsForType.close();
        psFieldsForType.close();
    }
    
    private void mapType(int dataId, int dbId) {
        map(typeMap, dataId, dbId);
    }
    
    private void mapMethod(int dataId, int dbId) {
        map(methodMap, dataId, dbId);
    }
    
    private void mapField(int dataId, int dbId) {
        map(fieldMap, dataId, dbId);
    }
    
    private <T> void map(List<T> map, int dataId, T value) {
        while (map.size() <= dataId) {
            map.addAll((List) NULL_1K);
        }
        map.set(dataId, value);
    }
    
    private static final List<?> NULL_1K = new ArrayList<Object>(){
        private final Object[] o = new Object[1024];
        { addAll(Arrays.asList(o)); }
        @Override
        public Object[] toArray() {
            return o;
        }
    };
    
    private static final int TYPE_ID = Database.csvFieldIndex("Type", "id");
    private static final int TYPE_NAME = Database.csvFieldIndex("Type", "name");
    private static final int METHOD_ID = Database.csvFieldIndex("Method", "id");
    private static final int METHOD_NAME = Database.csvFieldIndex("Method", "name");
    private static final int METHOD_SIG = Database.csvFieldIndex("Method", "signature");
    private static final int METHOD_HASH = Database.csvFieldIndex("Method", "hashcode");
    private static final int METHOD_TYPE = Database.csvFieldIndex("Method", "declaringTypeId");
    private static final int VARIABLE_METHOD = Database.csvFieldIndex("Variable", "methodId");
    private static final int FIELD_ID = Database.csvFieldIndex("Field", "id");
    private static final int FIELD_NAME = Database.csvFieldIndex("Field", "name");
    private static final int FIELD_TYPE = Database.csvFieldIndex("Field", "declaringTypeId");
    private static final int TRACE_ID = Database.csvFieldIndex("TestTrace", "id");
    private static final int OBJECT_TRACE = Database.csvFieldIndex("ObjectTrace", "testId");
    private static final int OBJECT_TYPE = Database.csvFieldIndex("ObjectTrace", "typeId");
    private static final int CALL_TRACE = Database.csvFieldIndex("CallTrace", "testId");
    private static final int CALL_METHOD = Database.csvFieldIndex("CallTrace", "methodId");
    private static final int EXIT_TRACE = Database.csvFieldIndex("ExitTrace", "testId");
    private static final int EXCEPTION_TRACE = Database.csvFieldIndex("ThrowTrace", "testId");
    private static final int VAR_SET_TRACE = Database.csvFieldIndex("VariableTrace", "testId");
    private static final int VAR_SET_METHOD = Database.csvFieldIndex("VariableTrace", "methodId");
    private static final int FIELD_ACC_TRACE = Database.csvFieldIndex("PutTrace", "testId");
    private static final int FIELD_ACC_FIELD = Database.csvFieldIndex("PutTrace", "fieldId");

    private class TypeData {

        private final Map<String, Integer> methods = new HashMap<>();
        private final Map<String, Integer> fields = new HashMap<>();
        
        public TypeData(int dbTypeId) throws SQLException {
            psMethodsForType.setInt(1, dbTypeId);
            try (ResultSet rs = psMethodsForType.executeQuery()) {
                while (rs.next()) {
                    String methodKey =
                            rs.getString("name") +
                            rs.getString("signature") +
                            Long.toHexString(rs.getLong("hashcode"));
                    int id = rs.getInt("id");
                    methods.put(methodKey, id);
                }
            }
            psFieldsForType.setInt(1, dbTypeId);
            try (ResultSet rs = psFieldsForType.executeQuery()) {
                while (rs.next()) {
                    String fieldKey = rs.getString("name");
                    int id = rs.getInt("id");
                    fields.put(fieldKey, id);
                }
            }
        }

        private int getMethodId(String methodKey) {
            Integer i = methods.get(methodKey);
            if (i == null) return -1;
            return i;
        }

        private int getFieldId(String fieldKey) {
            Integer i = fields.get(fieldKey);
            if (i == null) return -1;
            return i;
        }
    }
}
