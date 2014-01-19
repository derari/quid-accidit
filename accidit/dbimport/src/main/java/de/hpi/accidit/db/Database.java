package de.hpi.accidit.db;

import au.com.bytecode.opencsv.CSVReader;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class Database {
    
    private final Connection cnn;
    private final String dbType;
    private final String dbSchema;
    private final Map<String, String> replace = new HashMap<>();
    private final Import batchImport;

    public Database(Connection cnn, String dbType, String schema) {
        this.cnn = cnn;
        this.dbType = dbType;
        this.dbSchema = schema;
        replace.put("$SCHEMA$", schema);
        switch (dbType) {
            case "hana":
                batchImport = new InsertScriptImport("INSERT INTO \"$SCHEMA$\".\"$TABLE$\" ($FIELDS$) VALUES ($VALUES$)", "\"$FIELD$\"");
                break;
            default:
                batchImport = new CsvScriptImport();
        }
    }
    
    public void createSchema() throws Exception {
        runSql(cnn, dbType + "/schema.sql", replace);
    }
    
    public boolean rewriteRequired() {
        return dbType.equals("monet");
    }

    public void importData(String csvDir) throws Exception {
        batchImport.importData(csvDir);
    }
    
    private static void runSql(Connection cnn, String sqlFile, Map<String, String> replace) throws Exception {
        boolean ignoreError = false;
        try (Statement stmt = cnn.createStatement()) {
            InputStream is = Database.class.getClassLoader().getResourceAsStream("de/hpi/accidit/db/" + sqlFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            StringBuilder query = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    if (query.length() > 0) {
                        execute(stmt, query.toString(), replace, ignoreError);
                        query.setLength(0);
                        ignoreError = false;
                    }
                } else if (line.startsWith("--try")) {
                    ignoreError = true;
                } else if (line.startsWith("--")) {
                    // ignore comment
                } else {
                    query.append(line).append(" ");
                }
            }
            if (query.length() > 0) {
                execute(stmt, query.toString(), replace, ignoreError);
                query.setLength(0);
            }
        }
    }

    private static void execute(Statement stmt, String query, Map<String, String> replace, boolean ignoreError) throws SQLException {
        for (Map.Entry<String, String> r: replace.entrySet()) {
            query = query.replace(r.getKey(), r.getValue());
        }
        System.out.printf("%s%n", query);
        long time = System.currentTimeMillis();
        try {
            boolean resultSet = stmt.execute(query);
            time = System.currentTimeMillis() - time;
            int size;
            if (resultSet) {
                size = stmt.getResultSet().getFetchSize();
            } else {
                size = stmt.getUpdateCount();
            }
            System.out.printf("> %d (%.3f s) %n", size, time/1000d);
        } catch (SQLException e) {
            time = System.currentTimeMillis() - time;
            System.out.printf("> %s (%.3f s) %n", e.getMessage(), time/1000d);
            if (!ignoreError) throw e;
        }
    }
    
    protected abstract static class Import {
        public abstract void importData(String csvDir) throws Exception; 
    }
    
    protected class CsvScriptImport extends Import {
        @Override
        public void importData(String csvDir) throws Exception {
            Map<String, String> replace2 = new HashMap<>(replace);
            replace2.put("$CSVDIR$", csvDir.replace('\\', '/'));
            runSql(cnn, dbType + "/csvimport.sql", replace2);
        }
    }
    
    protected class InsertScriptImport extends Import {
        
        private final String sqlTemplate;
        private final String fieldTemplate;

        public InsertScriptImport(String sqlTemplate, String fieldTemplate) {
            this.sqlTemplate = sqlTemplate;
            this.fieldTemplate = fieldTemplate;
        }
        
        private void importData(File data, String table, String[] fields, String[] types) throws Exception {
            final int MAX_BATCH = 1 << 16;
            long total = 0;
            long curBatch = 0;
            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(data), "utf-8"), ';');
            long time = System.currentTimeMillis();
            try (PreparedStatement ps = cnn.prepareStatement(makeQueryString(sqlTemplate, fieldTemplate, dbSchema, table, fields))) {
                String[] row = reader.readNext();
                while (row != null) {
                    setArgs(ps, types, row);
                    ps.addBatch();
                    curBatch++;
                    if (curBatch >= MAX_BATCH) {
                        ps.executeBatch();
                        total += curBatch;
                        System.out.printf("> %7d (+%d) (%.3f s) %n", total, curBatch, (System.currentTimeMillis() - time)/1000d);
                        curBatch = 0;
                    }
                    row = reader.readNext();
                }
                ps.executeBatch();
                total += curBatch;
                System.out.printf("> %7d (+%d) (%.3f s) %n", total, curBatch, (System.currentTimeMillis()- time)/1000d);
            }
        }
        
        private void setArgs(PreparedStatement ps, String[] types, String[] values) throws SQLException {
            for (int i = 0; i < types.length; i++) {
                String t = types[i];
                String v = values[i];
                if (t.endsWith("?") && (v == null || v.equals("NULL"))) {
                    int st;
                    switch (t.charAt(0)) {
                        case 's':
                            st = Types.VARCHAR;
                            break;
                        case 'c':
                            st = Types.CHAR;
                            break;
                        case 'i':
                            st = Types.INTEGER;
                            break;
                        case 'l':
                            st = Types.BIGINT;
                            break;
                        default:
                            throw new IllegalArgumentException(t);
                    }
                    ps.setNull(i+1, st);
                } else {
                    switch (t.charAt(0)) {
                        case 's':
                        case 'c':
                            ps.setString(i+1, v);
                            break;
                        case 'i':
                            ps.setInt(i+1, Integer.parseInt(v));
                            break;
                        case 'l':
                            ps.setLong(i+1, Long.parseLong(v));
                            break;
                        default:
                            throw new IllegalArgumentException(t);
                    }
                }
            }
        }
        @Override
        public void importData(String csvDir) throws Exception {
            for (String[][] table: TABLES) {
                File f = new File(csvDir, table[0][1]);
                importData(f, table[0][0], table[1], table[2]);
            }
        }
    }
    
    public class BulkImport implements AutoCloseable {
        
        private final String sqlTemplate;
        private final String fieldTemplate;
        private String query = null;
        private String[] types = null;
        private boolean close;

        public BulkImport(String sqlTemplate, String fieldTemplate) {
            this.sqlTemplate = sqlTemplate;
            this.fieldTemplate = fieldTemplate;
        }
        
        public void setTable(String table) {
            for (String[][] meta: TABLES) {
                if (meta[0][0].equals(table)) {
                    query = makeQueryString(sqlTemplate, fieldTemplate, dbSchema, table, meta[1]);
                    types = meta[2];
                    return;
                }
            }
        }

        @Override
        public void close() {
            
        }
    }
    
    private static final String[][][] TABLES = {
                {{"Type", "mType.csv"},
                    {"id", "name", "file", "componentTypeId"},
                    {"i", "s", "s", "i?"}},
                {{"Extends", "mExtends.csv"},
                    {"subId", "superId"},
                    {"i", "i"}},
                {{"Field", "mField.csv"},
                    {"id", "declaringTypeId", "name", "typeId"},
                    {"i", "i", "s", "i"}},
                {{"Method", "mMethod.csv"},
                    {"id", "declaringTypeId", "name", "signature", "line", "hashcode"},
                    {"i", "i", "s", "s", "i?", "l"}},
                {{"Variable", "mVariable.csv"},
                    {"methodId", "id", "name", "typeId", "parameter"},
                    {"i", "i", "s", "i", "i"}},
                {{"TestTrace", "tTrace.csv"},
                    {"id", "name"},
                    {"i", "s"}},
                {{"ObjectTrace", "tObject.csv"},
                    {"testId", "id", "typeId", "arrayLength"},
                    {"i", "l", "i", "i?"}},
                {{"CallTrace", "tCall.csv"},
                    {"testId", "parentStep", "step", "exitStep", "methodId", "thisId", "depth", "line"},
                    {"i", "l?", "l", "l", "i", "l?", "i", "i?"}},
                {{"ExitTrace", "tExit.csv"},
                    {"testId", "step", "returned", "primType", "valueId", "line"},
                    {"i", "l", "i", "c", "l", "i?"}},
                {{"ThrowTrace", "tThrow.csv"},
                    {"testId", "callStep", "step", "exceptionId", "line"},
                    {"i", "l", "l", "l", "i?"}},
                {{"CatchTrace", "tCatch.csv"},
                    {"testId", "callStep", "step", "exceptionId", "line"},
                    {"i", "l", "l", "l", "i?"}},
                {{"VariableTrace", "tVariable.csv"},
                    {"testId", "callStep", "step", "methodId", "variableId", "primType", "valueId", "line"},
                    {"i", "l", "l", "i", "i", "c", "l", "i?"}},
                {{"PutTrace", "tPut.csv"},
                    {"testId", "callStep", "step", "thisId", "fieldId", "primType", "valueId", "line"},
                    {"i", "l", "l", "l?", "i", "c", "l", "i?"}},
                {{"GetTrace", "tGet.csv"},
                    {"testId", "callStep", "step", "thisId", "fieldId", "primType", "valueId", "line"},
                    {"i", "l", "l", "l?", "i", "c", "l", "i?"}},
                {{"ArrayPutTrace", "tArrayPut.csv"},
                    {"testId", "callStep", "step", "thisId", "index", "primType", "valueId", "line"},
                    {"i", "l", "l", "l", "i", "c", "l", "i?"}},
                {{"ArrayGetTrace", "tArrayGet.csv"},
                    {"testId", "callStep", "step", "thisId", "index", "primType", "valueId", "line"},
                    {"i", "l", "l", "l", "i", "c", "l", "i?"}},
            };
    
    private static String makeQueryString(String sqlTemplate, String fieldTemplate, String dbSchema, String table, String[] fields) {
        StringBuilder sbFields = new StringBuilder();
        StringBuilder sbValues = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sbFields.append(",");
                sbValues.append(",");
            }
            sbFields.append(fieldTemplate
                    .replace("$FIELD$", fields[i]));
            sbValues.append("?");
        }
        String q = sqlTemplate
                .replace("$SCHEMA$", dbSchema)
                .replace("$TABLE$", table)
                .replace("$FIELDS$", sbFields)
                .replace("$VALUES$", sbValues)
                ;
        System.out.println(q);
        return q;
    }
}
