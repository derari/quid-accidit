package de.hpi.accidit.eclipse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.eclipse.jface.preference.IPreferenceStore;

import de.hpi.accidit.eclipse.preferences.PreferenceConstants;
import de.hpi.accidit.orm.OConnection;

public class DatabaseConnector {
	
	private final static String MYSQL_DATABASE_DRIVER = "com.mysql.jdbc.Driver";
	
	private volatile static boolean initialized = false;
	
	/**
	 * The function to create a database connection.
	 * 
	 * @param dbAddress The IP address of the database.
	 * @param dbUser The user that should be used to connect to the database.
	 * @param dbPassword The password associated with the user.
	 * @return The database connection.
	 */
	public static Connection getConnection(String dbAddress, String dbSchema, String dbUser, String dbPassword) throws SQLException {
		if (!initialized)
			initializeDriver();
		
		String dbString = String.format("jdbc:mysql://%s/%s?user=%s&password=%s", dbAddress, dbSchema, dbUser, dbPassword);
		return DriverManager.getConnection(dbString);
	}
	
	private static String getDBString() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String dbAddress	= store.getString(PreferenceConstants.CONNECTION_ADDRESS);
		String dbSchema		= store.getString(PreferenceConstants.CONNECTION_SCHEMA);
		String dbUser		= store.getString(PreferenceConstants.CONNECTION_USER);
		String dbPassword	= store.getString(PreferenceConstants.CONNECTION_PASSWORD);
		
		String dbString = String.format("jdbc:mysql://%s/%s?user=%s&password=%s", dbAddress, dbSchema, dbUser, dbPassword);
		return dbString;
	}
	
	public static Connection getValidConnection() throws SQLException {
		String dbString = getDBString();
		return DriverManager.getConnection(dbString);
	}
	
	private static String lastDbString = null;
	private static OConnection cnn = null;
	
	public static synchronized OConnection getValidOConnection() {
		String dbString = getDBString();
		if (!dbString.equals(lastDbString)) {
			try {
				if (cnn != null) {
					cnn.close();
				}
				lastDbString = dbString;
				cnn = new OConnection(getValidConnection());
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		return cnn;
	}
	
	public static OConnection cnn() {
		return getValidOConnection();
	}
	
	/**
	 * Returns whether the database connection parameters allow to create a database connection.
	 * 
	 * @param dbAddress The IP address of the database.
	 * @param dbUser The user that should be used to connect to the database.
	 * @param dbPassword The password associated with the user.
	 * @return true if it is possible to establish a database connection using the given parameters, and false otherwise.
	 */
	public static boolean testConnection(String dbAddress, String dbSchema, String dbUser, String dbPassword) {
		try {
			Connection c = getConnection(dbAddress, dbSchema, dbUser, dbPassword);
			c.close();
		} catch (SQLException e) {
			return false;
		}
		return true;
	}
	
	private static void initializeDriver() {
		try {
			Class.forName(MYSQL_DATABASE_DRIVER);
		} catch (ClassNotFoundException e) {
			System.err.println("Exiting as there is no database driver available.");
			e.printStackTrace();
			System.exit(0);
		}
		initialized = true;
	}
}
