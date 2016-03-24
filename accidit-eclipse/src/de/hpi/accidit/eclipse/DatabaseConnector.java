package de.hpi.accidit.eclipse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cthul.miro.MiConnection;
import org.cthul.miro.MiConnection.QueryPreProcessor;
import org.cthul.miro.query.QueryType;
import org.cthul.miro.query.adapter.AbstractQueryBuilder;
import org.cthul.miro.query.adapter.JdbcAdapter;
import org.cthul.miro.query.parts.QueryPart;
import org.cthul.miro.query.sql.AnsiSql;
import org.cthul.miro.query.sql.BasicQuery;
import org.cthul.miro.query.sql.DataQuery;
import org.cthul.miro.query.sql.DataQueryPart;
import org.cthul.miro.query.sql.StringQueryBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;

import de.hpi.accidit.eclipse.properties.DatabaseSettingsPreferencePage;
import de.hpi.accidit.eclipse.properties.DatabaseSettingsRetriever;
import de.hpi.accidit.eclipse.views.util.Timer;

public class DatabaseConnector {
	
	private volatile static boolean initialized = false;
	private static String overrideDBString = null;
	private static String overrideSchema = null;
	
//	/**
//	 * The function to create a database connection.
//	 * 
//	 * @param dbAddress The IP address of the database.
//	 * @param dbUser The user that should be used to connect to the database.
//	 * @param dbPassword The password associated with the user.
//	 * @return The database connection.
//	 */
//	public static Connection getTestConnection(String dbAddress, String dbSchema, String dbUser, String dbPassword) throws SQLException {
//		if (!initialized) {
//			initializeDriver();
//		}
//		
//		String dbString = String.format("jdbc:mysql://%s/%s", dbAddress, dbSchema);
//		return DriverManager.getConnection(dbString, dbUser, dbPassword);
//	}
	
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
	
	public static synchronized MiConnection getValidOConnection() {
		String dbString = getDBString();
		try {
			if (cnn == null || !dbString.equals(lastDbString) || cnn.isClosed()) {
				if (cnn != null && !cnn.isClosed()) {
					cnn.close();
				}
				
				
				CustomDialect adapter = null;
				String dbSchema = getSchema();
				if (dbString.startsWith("jdbc:sap")) {
					adapter = new HanaDialect(dbSchema);
				} else if (dbString.startsWith("jdbc:mysql")) {
					adapter = new MySqlDialect(dbSchema);
				} else {
					adapter = new AnsiSqlDialect(dbSchema);
				}
				
				lastDbString = dbString;
				cnn = new MiConnection(adapter, newConnection());
				cnn.addPreProcessor(adapter);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return cnn;
	}
	
	public static MiConnection cnn() {
		return getValidOConnection();
	}
	
//	/**
//	 * Returns whether the database connection parameters allow to create a database connection.
//	 * 
//	 * @param dbAddress The IP address of the database.
//	 * @param dbUser The user that should be used to connect to the database.
//	 * @param dbPassword The password associated with the user.
//	 * @return true if it is possible to establish a database connection using the given parameters, and false otherwise.
//	 */
//	public static boolean testConnection(String dbAddress, String dbSchema, String dbUser, String dbPassword) {
//		try {
//			Connection c = getTestConnection(dbAddress, dbSchema, dbUser, dbPassword);
//			c.close();
//		} catch (SQLException e) {
//			e.printStackTrace(System.err);
//			return false;
//		}
//		return true;
//	}
	
//	private static void initializeDriver() {
////		try {
////			Class.forName(MYSQL_DATABASE_DRIVER);
////		} catch (ClassNotFoundException e) {
////			e.printStackTrace();
////		}
//		initialized = true;
//	}

	/* private classes for query preprocessing. */
	
	private static class CustomDialect extends AnsiSql implements QueryPreProcessor {

		@Override
		public String apply(String sql) {
			return postProcess(sql);
		}
	}
	
	private static class AnsiSqlDialect extends CustomDialect {
		
		private final String schema;
		
		public AnsiSqlDialect(String schema) {
			super();
			this.schema = schema;
		}
		
		@Override
		protected String postProcess(String sql) {
			sql = sql.replaceAll("\\s`(\\w+Trace|Type|Method|Variable|Field)`", " `SCHEMA`.`$1`")
					.replace("`SCHEMA`", "`" + schema + "`")
					  .replace("`", "\"")
					  .replaceAll("__ISNOTNULL\\{(.*?)\\}", "($1 IS NOT NULL)");
			System.out.println(sql);
			return sql;
		}
	};
	
	private static class HanaDialect extends CustomDialect {
		
		private final String schema;
		
		public HanaDialect(String schema) {
			super();
			this.schema = schema;
		}
		
		@Override
		protected String postProcess(String sql) {
			sql = sql.replace("`SCHEMA`", "`" + schema + "`")
					  .replace("`", "\"")
					  .replaceAll("__ISNOTNULL\\{(.*?)\\}", "(LEAST(0, IFNULL($1, -1))+1)");
			//System.out.println(sql);
			return sql;
		}
	};
	
	private static class MySqlDialect extends CustomDialect {
		
		private final String schema;
		
		public MySqlDialect(String schema) {
			super();
			this.schema = schema;
		}
		
		@Override
		protected String postProcess(String sql) {
			sql = sql.replace("`SCHEMA`", "`" + schema + "`")
					  .replaceAll("__ISNOTNULL\\{(.*?)\\}", "($1 IS NOT NULL)");
//			System.out.println(sql);
			return sql;
		}
		
		protected <T> T newQueryBuilder(QueryType<?> queryType) {
	        if (queryType instanceof DataQuery.Type) {
	            switch ((DataQuery.Type) queryType) {
	                case SELECT:
	                    return (T) new MySelectQuery(this);
                    default:
	            }
	        }
	        if (queryType == BasicQuery.STRING) {
	            return (T) new MyStringQuery();
	        }
	        return super.newQueryBuilder(queryType);
	    }
		
		public static class MySelectQuery extends SelectQuery {

			public MySelectQuery(AnsiSql dialect) {
				super(dialect);
			}
			
			@Override
			public ResultSet execute(Connection connection) throws SQLException {
				Timer.Job tt = new Timer.Job(getQueryString(), getArguments(0).toArray());
				try {
					return super.execute(connection);
				} finally {
					tt.done();
				}
			}
		}
		
		public static class MyStringQuery extends AbstractQueryBuilder<StringQueryBuilder<?>> implements StringQueryBuilder<StringQueryBuilder<?>> {

	        private final List<Object[]> batches = new ArrayList<>();
	        private String query = null;
	        
	        public MyStringQuery() {
	            super(0);
	        }

	        @Override
	        protected StringQueryBuilder<?> addPart(DataQueryPart type, QueryPart part) {
	            throw new UnsupportedOperationException();
	        }

	        @Override
	        protected void buildQuery(StringBuilder sql) {
	            sql.append(query);
	        }

	        @Override
	        protected void collectArguments(List<Object> args, int batch) {
	            args.addAll(Arrays.asList(batches.get(batch)));
	        }

	        @Override
	        public QueryType<StringQueryBuilder<?>> getQueryType() {
	            return BasicQuery.STRING;
	        }

	        @Override
	        public StringQueryBuilder<?> query(String query) {
	            this.query = query;
	            return this;
	        }

	        @Override
	        public StringQueryBuilder<?> batch(Object... values) {
	            batches.add(values);
	            return this;
	        }

	        @Override
	        public int getBatchCount() {
	            if (batches.size() == 1) return 0;
	            return batches.size();
	        }
	        
	        @Override
			public ResultSet execute(Connection connection) throws SQLException {
				Timer.Job tt = new Timer.Job(getQueryString(), getArguments(0).toArray());
				try {
					return super.execute(connection);
				} finally {
					tt.done();
				}
			}
	    }
		
		
		
	};
}
