package de.hpi.accidit.db;

import au.com.bytecode.opencsv.CSVReader;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                batchImport = new InsertScriptImport(insertIntoString(), fieldString());
                break;
            default:
                batchImport = new CsvScriptImport();
        }
    }
    
    private String insertIntoString() {
        switch (dbType) {
            case "mysql":
                return "INSERT INTO `$SCHEMA$`.`$TABLE$` ($FIELDS$) VALUES ($VALUES$)";
            default: 
                return "INSERT INTO \"$SCHEMA$\".\"$TABLE$\" ($FIELDS$) VALUES ($VALUES$)";
        }
    }
    
    private String fieldString() {
        switch (dbType) {
            case "mysql":
                return "`$FIELD$`";
            default: 
                return "\"$FIELD$\"";
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
    
    public BulkImport bulkImport() {
        return new BulkImport(insertIntoString(), fieldString());
    }
    
    public PreparedStatement prepare(String s) throws SQLException {
        return cnn.prepareStatement(s);
    }
    
    public int getMaxId(String table) throws SQLException {
        String sql = "SELECT MAX(id) FROM " + table;
        try (Statement stmt = cnn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
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
    
    protected abstract static class Import implements AutoCloseable {
        public abstract void importData(String csvDir) throws Exception; 

        @Override
        public abstract void close();
    }
    
    protected class CsvScriptImport extends Import {
        @Override
        public void importData(String csvDir) throws Exception {
            Map<String, String> replace2 = new HashMap<>(replace);
            replace2.put("$CSVDIR$", csvDir.replace('\\', '/'));
            runSql(cnn, dbType + "/csvimport.sql", replace2);
        }

        @Override
        public void close() {
        }
    }
    
    protected class InsertScriptImport extends Import {
        
//        private final String sqlTemplate;
//        private final String fieldTemplate;
        private final BulkImport bulk;

        public InsertScriptImport(String sqlTemplate, String fieldTemplate) {
            this.bulk = new BulkImport(sqlTemplate, fieldTemplate);
        }
        
        @Override
        public void importData(String csvDir) throws Exception {
            for (String[][] table: TABLES) {
                File f = new File(csvDir, table[0][1]);
                importData(f, table[0][0]);
            }
        }
        
        private void importData(File data, String table) throws Exception {
            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(data), "utf-8"), ';');
            bulk.setTable(table);
            String[] row = reader.readNext();
            while (row != null) {
                bulk.addRow(row);
                row = reader.readNext();
            }
        }

        @Override
        public void close() {
            bulk.close();
        }
    }

    private static int MAX_INSERT = 1 << 16;
    
    public class BulkImport implements AutoCloseable {
        
        private final String sqlTemplate;
        private final String fieldTemplate;
        private final List<String[]> rows = new ArrayList<>(MAX_INSERT);
        private final Thread thread;
        private long startTime = 0;
        private long total = 0;
        private String[][] makeNextStatement = null;
        private PreparedStatement ps = null;
        private int[] types = null;
        private boolean[] nullable = null;
        private volatile boolean closed = false;
        private boolean submitted = false;

        public BulkImport(String sqlTemplate, String fieldTemplate) {
            this.sqlTemplate = sqlTemplate;
            this.fieldTemplate = fieldTemplate;
            this.thread = setUpThread();
        }
        
        private Thread setUpThread() {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        String[][] curTable = {};
                        String[][] buf = {};
                        int count;
                        while (!closed) {
                            synchronized (rows) {
                                if (!submitted) {
                                    rows.wait();
                                }
                                count = rows.size();
                                buf = rows.toArray(buf);
                                rows.clear();
                                if (count > 0 && makeNextStatement != curTable) {
                                    curTable = makeNextStatement;
                                    makeStatement(curTable);
                                }
                                submitted = false;
                                rows.notifyAll();
                            }
                            if (count > 0) {
                                insertData(buf, count);
                            }
                        }
                    } catch (InterruptedException ex) {
                        // exit thread
                    } catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                    closed = true;
                }
            };
            t.start();
            return t;
        }
        
        public void setTable(String table) throws SQLException {
            submitData();
            for (String[][] meta: TABLES) {
                if (meta[0][0].equals(table)) {
                    setTable(meta);
                    return;
                }
            }
            throw new IllegalArgumentException(table);
        }
        
        private void setTable(String[][] meta) throws SQLException {
            startTime = System.currentTimeMillis();
//            makeStatement(meta);
            makeNextStatement = meta;
        }
        
        private void makeStatement(String[][] meta) throws SQLException {
            if (ps != null) {
                ps.close();
            }
            String[] typeList = meta[2];
            types = new int[typeList.length];
            nullable = new boolean[typeList.length];
            for (int i = 0; i < typeList.length; i++) {
                String t = typeList[i];
                types[i] = getType(t);
                nullable[i] = isNullable(t);
            }
            String query = makeQueryString(sqlTemplate, fieldTemplate, dbSchema, meta[0][0], meta[1]);
            System.out.println(query);
            ps = cnn.prepareStatement(query);
        }
        
        private int getType(String t) {
            switch (t.charAt(0)) {
                case 's':
                    return Types.VARCHAR;
                case 'c':
                    return Types.CHAR;
                case 'i':
                    return Types.INTEGER;
                case 'l':
                    return Types.BIGINT;
                default:
                    throw new IllegalArgumentException(t);
            }
        }
        
        private boolean isNullable(String t) {
            return t.endsWith("?");
        }
        
        public void addRow(String[] row) {
            if (closed) {
                throw new IllegalStateException("closed");
            }
            rows.add(row);
            if (rows.size() >= MAX_INSERT) {
                submitData();
            }
        }

        private void submitData() {
            if (rows.isEmpty() || closed) return;
            synchronized (rows) {
                submitted = true;
                rows.notifyAll();
                try {
                    rows.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        private void insertData(final String[][] data, final int count) throws SQLException {
            for (int i = 0; i < count; i++) {
                insertRow(data[i]);
                ps.addBatch();
            }
            ps.executeBatch();
            total += count;
            System.out.printf("> %7d (+%d) (%.3f s) %n", total, count, (System.currentTimeMillis()- startTime)/1000d);
        }

        private void insertRow(final String[] row) throws SQLException {
            for (int i = 0; i < row.length; i++) {
                String v = row[i];
                if (nullable[i] && (v == null || v.equals("NULL"))) {
                    ps.setNull(i+1, types[i]);
                } else {
                    ps.setObject(i+1, v, types[i]);
                }
            }
        }
        
        @Override
        public void close() {
            submitData();
            synchronized (rows) {
                closed = true;
                rows.notifyAll();
            }
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
    
    public static int csvFieldIndex(String table, String field) {
        for (String[][] meta: TABLES) {
            if (meta[0][0].equals(table)) {
                String[] fields = meta[1];
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].equals(field)) {
                        return i;
                    }
                }
            }
        }
        throw new IllegalArgumentException(table + "." + field);
    }
    
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
        //System.out.println(q);
        return q;
    }
}
