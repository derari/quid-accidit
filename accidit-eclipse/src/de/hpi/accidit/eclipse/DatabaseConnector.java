package de.hpi.accidit.eclipse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.db.MiException;
import org.cthul.miro.db.syntax.Syntax;
import org.cthul.miro.db.syntax.SyntaxProvider;
import org.cthul.miro.ext.jdbc.JdbcConnection;
import org.cthul.miro.ext.jdbc.JdbcConnection.ConnectionProvider;
import org.cthul.miro.ext.mysql.MySqlSyntax;
import org.cthul.miro.sql.syntax.AnsiSqlSyntax;
import org.eclipse.core.resources.IProject;

import de.hpi.accidit.eclipse.model.db.TraceDB;
import de.hpi.accidit.eclipse.properties.DatabaseSettingsPreferencePage;
import de.hpi.accidit.eclipse.properties.DatabaseSettingsRetriever;
import de.hpi.accidit.eclipse.views.util.Timer;

public class DatabaseConnector {
	
	private volatile static boolean initialized = false;
	private static String overrideDBString = null;
	private static String overrideSchema = null;
	
	private static IProject selectedProject = null;
	
	public static IProject getSelectedProject() {
		return selectedProject;
	}

	public static void setSelectedProject(IProject project) {
		selectedProject = project;
	}
	
	public static void overrideDBString(String string) {
		overrideDBString = string;
	}
	
	public static void overrideSchema(String string) {
		overrideSchema = string;
	}

	public static String getSchema() {
		if (overrideSchema != null) return overrideSchema;
		IProject project = selectedProject;
		return DatabaseSettingsRetriever
				.getPreferenceValue(project, DatabaseSettingsPreferencePage.CONNECTION_SCHEMA);		
	}
	
	public static String getDBType() {
		IProject project = selectedProject;
		return DatabaseSettingsRetriever
				.getPreferenceValue(project, DatabaseSettingsPreferencePage.CONNECTION_TYPE);		
	}
	
	public static String getDBString() {
		if (overrideDBString != null) return overrideDBString;
		IProject project = selectedProject;
		
		String dbAddress = DatabaseSettingsRetriever
				.getPreferenceValue(project, DatabaseSettingsPreferencePage.CONNECTION_ADDRESS);
		String dbSchema	= getSchema();
		String dbType = getDBType();
		if (dbType.equals("hsqldb")) {
			return String.format("jdbc:hsqldb:%s;default_schema=%s", dbAddress, dbSchema);
		}
		return String.format("jdbc:%s://%s/%s?currentschema=%s", dbType, dbAddress, dbSchema, dbSchema);
	}
	
	public static Connection newConnection() throws SQLException {
		IProject project = selectedProject;
		String dbString = getDBString();
		String dbUser = DatabaseSettingsRetriever
				.getPreferenceValue(project, DatabaseSettingsPreferencePage.CONNECTION_USER);
		String dbPassword = DatabaseSettingsRetriever
				.getPreferenceValue(project, DatabaseSettingsPreferencePage.CONNECTION_PASSWORD);
		return DriverManager.getConnection(dbString, dbUser, dbPassword);
	}
	
	private static String lastDbString = null;
	private static MiConnection cnn = null;
	private static TraceDB traceDb = null;
	
	private static final String[] TABLES = {
			"Type", "Method", "Variable", "Field",
			"TestTrace", "CallTrace", "ExitTrace",
			"ObjectTrace", "ThrowTrace", "CatchTrace", 
			"VariableTrace", "GetTrace", "PutTrace",
	};
	
	public static TraceDB getTraceDB() {
		String dbString = getDBString();
		try {
			if (traceDb == null || !dbString.equals(lastDbString)) {
				if (cnn != null) {
					cnn.close();
				}
				AnsiSqlSyntax syntax;
				if (dbString.contains("mysql")) {
					syntax = new MySqlSyntax();
				} else if (dbString.contains("hsqldb")) {
					syntax = new AnsiSqlSyntax();
				} else {
					syntax = SyntaxProvider.find(AnsiSqlSyntax.class, dbString);
				}
				syntax.schema(getSchema(), TABLES);
				ConnectionProvider cp = () ->  newConnection();
				cnn = new TimedJdbcConnection(cp.cached(), syntax);
				traceDb = new TraceDB(cnn, getSchema());
				lastDbString = dbString;
			}
		} catch (MiException e) {
			throw new RuntimeException(e);
		}
		return traceDb;
	}
	
	public static MiConnection cnn() {
		getTraceDB();
		return cnn;
	}
	
	private static class TimedJdbcConnection extends JdbcConnection {

		public TimedJdbcConnection(ConnectionProvider connectionSupplier, Syntax syntax) {
			super(connectionSupplier, syntax);
		}
		
		@Override
		public PreparedStatement prepareStatement(String sql) throws MiException {
			sql = sql.replaceAll("__ISNOTNULL\\{(.*?)\\}", "($1 IS NOT NULL)");
			return super.prepareStatement(sql);
		}
		
		@Override
		public ResultSet executeQuery(PreparedStatement stmt) throws MiException {
			System.out.println(stmt.toString());
			Timer.Job tt = new Timer.Job(stmt.toString(), new Object[0]);
			try {
				return super.executeQuery(stmt);
			} finally {
				tt.done();
			}
		}
	}
}
