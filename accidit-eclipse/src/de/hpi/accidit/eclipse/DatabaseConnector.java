package de.hpi.accidit.eclipse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
	
	private final static String MYSQL_DATABASE_DRIVER = "com.mysql.jdbc.Driver";
	
	private volatile static boolean uninitialized = true;
	
	/**
	 * The function to create a database connection.
	 * 
	 * @param dbAddress The IP address of the database.
	 * @param dbUser The user that should be used to connect to the database.
	 * @param dbPassword The password associated with the user.
	 * @return The database connection.
	 */
	public static Connection getConnection(String dbAddress, String dbSchema, String dbUser, String dbPassword) throws SQLException {
		if (uninitialized)
			initializeDriver();
		
		String dbString = String.format("jdbc:mysql://%s/%s?user=%s&password=%s", dbAddress, dbSchema, dbUser, dbPassword);
		return DriverManager.getConnection(dbString);
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
			getConnection(dbAddress, dbSchema, dbUser, dbPassword);
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
		uninitialized = false;
	}
}
