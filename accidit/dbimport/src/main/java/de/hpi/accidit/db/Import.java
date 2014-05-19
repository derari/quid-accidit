package de.hpi.accidit.db;

import au.com.bytecode.opencsv.CSVReader;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

public class Import {
    
    // jdbc:mysql://localhost:3306/acc2?user=root&password=root&currentschema=acc2 C:/trace -n
    // jdbc:sap://VM-APM-HIWI.EAALAB.HPI.UNI-POTSDAM.DE:30015/ACC2?user=SYSTEM&password=manager&currentschema=ACC2 C:/trace -n
    
    public static void main(String... args) throws Exception {
        boolean newSchema = false;
        String dbType = null;
        String dbSchema = null;
        String dbString;
        String dbUser = null;
        String dbPassword = null;
        String csvDir = null;
        Queue<String> arguments = new ArrayDeque<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a.toLowerCase()) {
                case "-n":
                case "--new":
                    newSchema = true;
                    break;
                case "-s":
                    dbSchema = args[++i];
                    break;
                case "-t":
                    dbType = args[++i];
                    break;
                case "-d":
                    csvDir = args[++i];
                    break;
                case "-u":
                    dbUser = args[++i];
                    break;
                case "-p":
                    dbPassword = args[++i];
                    break;
                default:
                    arguments.add(a);
            }
        }
        int requiredArgs = csvDir == null ? 2 : 1;
        if (dbType == null && arguments.size() > requiredArgs) {
            dbType = arguments.remove();
        }
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("DB string expected");
        }
        requiredArgs--;
        dbString = arguments.remove();
        if (dbSchema == null && arguments.size() > requiredArgs) {
            dbSchema = arguments.remove();
        }
        if (csvDir == null) {
            if (arguments.isEmpty()) {
                throw new IllegalArgumentException("Data directory expected");
            }
            csvDir = arguments.remove();
        }
        if (!arguments.isEmpty()) {
            throw new IllegalArgumentException("Unexpected arguments: " + arguments);
        }
        if (dbType == null) dbType = detectType(dbString);
        if (dbSchema == null) dbSchema = detectSchema(dbString);
        if (dbUser == null && !(dbString.contains("user="))) {
            dbUser = System.console().readLine("User: ");
        }
        if (dbPassword == null && !(dbString.contains("password="))) {
            char[] pw = System.console().readPassword("Password: ");
            dbPassword = new String(pw);
        }
        Properties p = new Properties();
        if (dbUser != null) p.put("user", dbUser);
        if (dbPassword != null) p.put("password", dbPassword);
        if (dbSchema != null) p.put("currentschema", dbSchema);
        
        System.out.printf("Connecting to: %s%n", guessHost(dbString));
        System.out.printf("Type: %s%n", dbType);
        System.out.printf("Schema: %s%n", dbSchema);
        System.out.printf("User: %s%n", dbUser);
        System.out.printf("Using Password: %s%n", dbPassword != null ?  "yes" : "no");
        System.out.printf("Clear Schema: %s%n", newSchema ?  "yes" : "no");
        
        Connection cnn = DriverManager.getConnection(dbString, p);
        
        new Import(dbType, cnn, dbSchema, csvDir, newSchema).run();
    }
    

    private static String detectType(String dbString) {
        if (dbString.startsWith("jdbc:mysql")) {
            return "mysql";
        }
        if (dbString.startsWith("jdbc:sap")) {
            return "hana";
        }
        throw new IllegalArgumentException("Unknown database type: " + dbString);
    }
    
    private static String detectSchema(String dbString) {
        final String currentSchemaKey = "currentschema=";
        int start = dbString.indexOf(currentSchemaKey);
        if (start < 0) {
            int i = dbString.indexOf("//");
            URI uri = URI.create(dbString.substring(i));
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                if (path.startsWith("/")) path = path.substring(1);
                return path;
            }
            throw new IllegalArgumentException("No schema given: " + dbString);
        }
        start += currentSchemaKey.length();
        int end = dbString.indexOf("&", start);
        if (end < 0) end = dbString.length();
        return dbString.substring(start, end);
    }
    
    private static String guessHost(String dbString) {
        int i = dbString.indexOf("//");
        if (i < 0) {
            i = dbString.indexOf("?");
            if (i < 0) {
                return dbString;
            } else {
                return dbString.substring(0, i);
            }
        } else {
            return URI.create("http:" + dbString.substring(i)).getHost();
        }
    }
    
//    private final String dbType;
//    private final String dbString;
//    private final String dbSchema;
    private String csvDir;
    private final boolean newSchema;
    private final Database db;
    private final List<Integer> newTraceIds = new ArrayList<>();
    
    private static Connection getConnection(String dbString, String dbSchema) throws SQLException {
        try {
            return DriverManager.getConnection(dbString);
        } catch (SQLException e) {
            try {
                // maybe schema doesnt exist?
                String dbString2 = dbString.replace("currentschema=" + dbSchema, "")
                                    .replace("/" + dbSchema, "");
                return DriverManager.getConnection(dbString2);
//                Connection c = DriverManager.getConnection(dbString);
//                Statement stmt = c.createStatement();
//                stmt.execute("CREATE SCHEMA ");
            } catch (SQLException e2) {
                e2.addSuppressed(e);
                throw e2;
            }
        }
    }
    
    public Import(String dbString, String csvDir, boolean newSchema) throws SQLException {
        this(dbString, detectSchema(dbString), csvDir, newSchema);
    }
    
    public Import(String dbString, String dbSchema, String csvDir, boolean newSchema) throws SQLException {
        this(detectType(dbString), dbString, dbSchema, csvDir, newSchema);
    }
    
    public Import(String dbType, String dbString, String dbSchema, String csvDir, boolean newSchema) throws SQLException {
        this(dbType, getConnection(dbString, dbSchema), dbSchema, csvDir, newSchema);
    }
    
    public Import(String dbType, Connection cnn, String dbSchema, String csvDir, boolean newSchema) throws SQLException {
//        this.dbType = dbType;
//        this.dbString = dbString;
//        this.dbSchema = dbSchema;
        this.csvDir = csvDir;
        this.newSchema = newSchema;
        db = new Database(cnn, dbType, dbSchema);
    }
    
    public List<Integer> run() throws Exception {
        long time = System.currentTimeMillis();
        if (csvDir.endsWith("*")) {
            multiRun();
        } else if (csvDir.endsWith("!")) {
            File f = new File(csvDir.substring(0, csvDir.length()-1));
            File[] dirs = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
            
        } else {
            singleRun();
        }
        time = System.currentTimeMillis() - time;
        System.out.printf("%nDONE. %ds%n", time/1000);
        return newTraceIds;
    }
    
    private void multiRun() throws Exception {
        if (newSchema) {
            db.createSchema();
        }
        
        manualImport();
    }
    
    private void singleRun() throws Exception {
        if (newSchema) {
            db.createSchema();
            db.importData(csvDir);
        } else {
            manualImport();
        }
    }
    
    private void manualImport() throws Exception {
        db.ensureSchemaExists();
        try (Model m = new Model(db, newTraceIds);
            RowIterator rType = reader("mType");
            RowIterator rMethod = reader("mMethod");
            RowIterator rVariable = reader("mVariable");
            RowIterator rField = reader("mField");
            RowIterator rTrace = reader("tTrace");
            RowIterator rObject = reader("tObject");
            RowIterator rCall = reader("tCall");
            RowIterator rExit = reader("tExit");
            RowIterator rThrow = reader("tThrow");
            RowIterator rCatch = reader("tCatch");
            RowIterator rVarSet = reader("tVariable");
            RowIterator rPut = reader("tPut");
            RowIterator rGet = reader("tGet");
            RowIterator rAPut = reader("tArrayPut");
            RowIterator rAGet = reader("tArrayGet");
           ) {
           m.beginTypes();
           for (String[] row: rType) {
               m.addType(row);
           }
           m.beginMethods();
           for (String[] row: rMethod) {
               m.addMethod(row);
           }
           m.beginVariables();
           for (String[] row: rVariable) {
               m.addVariable(row);
           }
           m.beginFields();
           for (String[] row: rField) {
               m.addField(row);
           }
           m.beginTraces();
           for (String[] row: rTrace) {
               m.addTrace(row);
           }
           m.beginObjects();
           for (String[] row: rObject) {
               m.addObject(row);
           }
           m.beginCalls();
           for (String[] row: rCall) {
               m.addCall(row);
           }
           m.beginExits();
           for (String[] row: rExit) {
               m.addExit(row);
           }
           m.beginThrows();
           for (String[] row: rThrow) {
               m.addException(row);
           }
           m.beginCatchs();
           for (String[] row: rCatch) {
               m.addException(row);
           }
           m.beginVariableSets();
           for (String[] row: rVarSet) {
               m.addVariableSet(row);
           }
           m.beginFieldPuts();
           for (String[] row: rPut) {
               m.addFieldAccess(row);
           }
           m.beginFieldGets();
           for (String[] row: rGet) {
               m.addFieldAccess(row);
           }
           m.beginArrayPuts();
           for (String[] row: rAPut) {
               m.addArrayAccess(row);
           }
           m.beginArrayGets();
           for (String[] row: rAGet) {
               m.addArrayAccess(row);
           }
       }
    }

    protected RowIterator reader(String file) throws UnsupportedEncodingException, FileNotFoundException {
        return new RowIterator(file);
    }
    
    public class RowIterator implements Iterable<String[]>, AutoCloseable {

        String file;
        CSVReader r = null;
        String[] next = null;

        public RowIterator(String file) {
            this.file = file;
        }
        
        private void fetch() {
            try {
                next = r.readNext();
                if (next == null) {
                    r.close();
                    r = null;
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        @Override
        public Iterator<String[]> iterator() {
            try {
                if (r != null) throw new IllegalStateException("iterator exists");
                r = new CSVReader(new InputStreamReader(new FileInputStream(csvDir + "/" + file + ".csv"), "utf-8"), ';');
                fetch();
            } catch (FileNotFoundException | UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
            return new Iterator<String[]>() {
                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public String[] next() {
                    String[] result = next;
                    fetch();
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public void close() throws Exception {
            if (r != null) r.close();
        }
        
    }
}
