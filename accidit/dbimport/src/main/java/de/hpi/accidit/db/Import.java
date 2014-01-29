package de.hpi.accidit.db;

import au.com.bytecode.opencsv.CSVReader;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
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
            URI uri = URI.create(dbString);
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
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
    private final String csvDir;
    private final boolean newSchema;
    private final Database db;
    
    public Import(String dbString, String csvDir, boolean newSchema) throws SQLException {
        this(dbString, detectSchema(dbString), csvDir, newSchema);
    }
    
    public Import(String dbString, String dbSchema, String csvDir, boolean newSchema) throws SQLException {
        this(detectType(dbString), dbString, dbSchema, csvDir, newSchema);
    }
    
    public Import(String dbType, String dbString, String dbSchema, String csvDir, boolean newSchema) throws SQLException {
        this(dbType, DriverManager.getConnection(dbString), dbSchema, csvDir, newSchema);
    }
    
    public Import(String dbType, Connection cnn, String dbSchema, String csvDir, boolean newSchema) throws SQLException {
//        this.dbType = dbType;
//        this.dbString = dbString;
//        this.dbSchema = dbSchema;
        this.csvDir = csvDir;
        this.newSchema = newSchema;
        db = new Database(cnn, dbType, dbSchema);
    }
    
    public void run() throws Exception {
        long time = System.currentTimeMillis();
        if (newSchema) {
            db.createSchema();
            db.importData(csvDir);
        } else {
            try (Model m = new Model(db);
                 CSVReader rType = reader("mType");) {
                m.beginTypes();
                String[] row = rType.readNext();
                while (row != null) {
                    m.addType(row);
                    row = rType.readNext();
                }
                m.beginMethods();
            }
        }
        time = System.currentTimeMillis() - time;
        System.out.printf("%nDONE. %ds%n", time/1000);
    }

    protected CSVReader reader(String file) throws UnsupportedEncodingException, FileNotFoundException {
        return new CSVReader(new InputStreamReader(new FileInputStream(csvDir + "/" + file + ".csv"), "utf-8"), ';');
    }
}
