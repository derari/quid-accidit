package de.hpi.accidit.eclipse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.cthul.miro.MiConnection;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;

import de.hpi.accidit.eclipse.preferences.PreferenceConstants;
import de.hpi.accidit.eclipse.properties.FieldEditorOverlayPage;

public class DatabaseConnector {
	
	private final static String MYSQL_DATABASE_DRIVER = "com.sap.db.jdbc.Driver";
//	private final static String MYSQL_DATABASE_DRIVER = "com.mysql.jdbc.Driver";
	
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
		
		String dbString = String.format("jdbc:mysql://%s/%s", dbAddress, dbSchema);
		return DriverManager.getConnection(dbString, dbUser, dbPassword);
	}
	
	private static String getDBString() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		// TODO: get resource from property
//		String foo = DatabaseSettingsRetriever.getOverlayedPreferenceValue(
//				store, 
//				resource, 
//				PreferenceConstants.CONNECTION_ADDRESS);
		
		
		
		String dbAddress	= store.getString(PreferenceConstants.CONNECTION_ADDRESS);
		String dbSchema		= store.getString(PreferenceConstants.CONNECTION_SCHEMA);
		String dbUser		= store.getString(PreferenceConstants.CONNECTION_USER);
		String dbPassword	= store.getString(PreferenceConstants.CONNECTION_PASSWORD);
		
		return String.format("jdbc:mysql://%s/%s?user=%s&password=%s&currentschema=%s", dbAddress, dbSchema, dbUser, dbPassword, dbSchema);
	}
	
	public static Connection getValidConnection() throws SQLException {
		String dbString = getDBString();
		return DriverManager.getConnection(dbString);
	}
	
	private static String lastDbString = null;
	private static MiConnection cnn = null;
	
	public static synchronized MiConnection getValidOConnection() {
		String dbString = getDBString();
		try {
			if (cnn == null || !dbString.equals(lastDbString) || cnn.isClosed()) {
					if (cnn != null && !cnn.isClosed()) {
						cnn.close();
					}
					lastDbString = dbString;
					cnn = new MiConnection(getValidConnection());
					IPreferenceStore store = Activator.getDefault().getPreferenceStore();
					String dbSchema		= store.getString(PreferenceConstants.CONNECTION_SCHEMA);
					if (dbString.startsWith("jdbc:sap")) {
						cnn.addPreProcessor(new HanaPP(dbSchema));
					} else if (dbString.startsWith("jdbc:mysql")) {
						cnn.addPreProcessor(new MySqlPP(dbSchema));
					}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return cnn;
	}
	
	public static MiConnection cnn() {
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
			e.printStackTrace(System.err);
			return false;
		}
		return true;
	}
	
	private static void initializeDriver() {
		try {
			Class.forName(MYSQL_DATABASE_DRIVER);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		initialized = true;
	}
	
	/* private class for property retrieving. */
	
	private static class DatabaseSettingsRetriever {
		
		private static final String DATABASE_SETTINGS_PREFEENCE_PAGE_ID = "de.hpi.accidit.eclipse.preferencePages.DatabaseSettings";
		
		public static String getOverlayedPreferenceValue(IPreferenceStore store, IResource resource, String key) {
			IProject project = resource.getProject();
			String value = null;
			if (useProjectSettings(project, DATABASE_SETTINGS_PREFEENCE_PAGE_ID)) {
				value = getProperty(resource, DATABASE_SETTINGS_PREFEENCE_PAGE_ID, key);
			}
			if (value != null)
				return value;
			return store.getString(key);
		}
		
		private static boolean useProjectSettings(IResource resource, String pageId) {
			String use = getProperty(resource, pageId, FieldEditorOverlayPage.USEPROJECTSETTINGS);
			return "true".equals(use);
		}
		
		private static String getProperty(IResource resource, 
				String pageId, 
				String key) {
			try {
				return resource.getPersistentProperty(
						new QualifiedName(pageId, key));
			} catch (CoreException e) {
			}
			return null;
		}
		
	}

	/* private classes for query preprocessing. */
	
	private static class HanaPP implements MiConnection.QueryPreProcessor {
		
		private final String schema;
		
		public HanaPP(String schema) {
			super();
			this.schema = schema;
		}
		
		@Override
		public String apply(String sql) {
			sql = sql.replace("`SCHEMA`", "`" + schema + "`")
					  .replace("`", "\"")
					  .replaceAll("__ISNOTNULL\\{(.*?)\\}", "(LEAST(0, IFNULL($1, -1))+1)");
			System.out.println(sql);
			return sql;
		}
	};
	
	private static class MySqlPP implements MiConnection.QueryPreProcessor {
		
		private final String schema;
		
		public MySqlPP(String schema) {
			super();
			this.schema = schema;
		}
		
		@Override
		public String apply(String sql) {
			sql = sql.replace("`SCHEMA`", "`" + schema + "`")
					  .replaceAll("__ISNOTNULL\\{(.*?)\\}", "($1 IS NOT NULL)");
			System.out.println(sql);
			return sql;
		}
	};
}
