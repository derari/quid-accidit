package de.hpi.accidit.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Queue;

public class Import {
    
    // jdbc:mysql://localhost:3306/acc2?user=root&password=root&currentschema=acc2 C:/trace -n
    // jdbc:sap://VM-APM-HIWI.EAALAB.HPI.UNI-POTSDAM.DE:30015/ACC2?user=SYSTEM&password=manager&currentschema=ACC2 C:/trace -n
    
    public static void main(String... args) throws Exception {
        boolean newSchema = false;
        String dbType = null;
        String dbSchema = null;
        String dbString;
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
                    dbSchema = a;
                    break;
                case "-t":
                    dbType = a;
                    break;
                case "-d":
                    csvDir = a;
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
        new Import(dbType, dbString, dbSchema, csvDir, newSchema).run();
    }
    
//    private final String dbType;
//    private final String dbString;
//    private final String dbSchema;
    private final String csvDir;
    private final boolean newSchema;
    private final Database db;

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
            throw new IllegalArgumentException("No schema given: " + dbString);
        }
        start += currentSchemaKey.length();
        int end = dbString.indexOf("&", start);
        if (end < 0) end = dbString.length();
        return dbString.substring(start, end);
    }
    
    public Import(String dbString, String csvDir, boolean newSchema) throws SQLException {
        this(dbString, detectSchema(dbString), csvDir, newSchema);
    }
    
    public Import(String dbString, String dbSchema, String csvDir, boolean newSchema) throws SQLException {
        this(detectType(dbString), dbString, dbSchema, csvDir, newSchema);
    }
    
    public Import(String dbType, String dbString, String dbSchema, String csvDir, boolean newSchema) throws SQLException {
//        this.dbType = dbType;
//        this.dbString = dbString;
//        this.dbSchema = dbSchema;
        this.csvDir = csvDir;
        this.newSchema = newSchema;
        Connection cnn = DriverManager.getConnection(dbString);
        db = new Database(cnn, dbType, dbSchema);
    }
    
    public void run() throws Exception {
        long time = System.currentTimeMillis();
        if (newSchema) {
            db.createSchema();
        }
        db.importData(csvDir);
        time = System.currentTimeMillis() - time;
        System.out.printf("%nDONE. %ds%n", time/1000);
    }
}
