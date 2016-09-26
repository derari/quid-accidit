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
			Timer.Job tt = new Timer.Job(stmt.toString(), new Object[0]);
			try {
				return super.executeQuery(stmt);
			} finally {
				tt.done();
			}
		}
	}
	
//	private static class CustomDialect extends AnsiSql implements QueryPreProcessor {
//
//		@Override
//		public String apply(String sql) {
//			return postProcess(sql);
//		}
//	}
	
//	private static class AnsiSqlDialect extends CustomDialect {
//		
//		private final String schema;
//		
//		public AnsiSqlDialect(String schema) {
//			super();
//			this.schema = schema;
//		}
//		
//		@Override
//		protected String postProcess(String sql) {
//			sql = sql.replaceAll("\\s`(\\w+Trace|Type|Method|Variable|Field)`", " `SCHEMA`.`$1`")
//					.replace("`SCHEMA`", "`" + schema + "`")
//					  .replace("`", "\"")
//					  .replaceAll("__ISNOTNULL\\{(.*?)\\}", "($1 IS NOT NULL)");
//			System.out.println(sql);
//			return sql;
//		}
//	};
//	
//	private static class HanaDialect extends CustomDialect {
//		
//		private final String schema;
//		
//		public HanaDialect(String schema) {
//			super();
//			this.schema = schema;
//		}
//		
//		@Override
//		protected String postProcess(String sql) {
//			sql = sql.replace("`SCHEMA`", "`" + schema + "`")
//					  .replace("`", "\"")
//					  .replaceAll("__ISNOTNULL\\{(.*?)\\}", "(LEAST(0, IFNULL($1, -1))+1)");
//			//System.out.println(sql);
//			return sql;
//		}
//	};
//	
//	private static class MySqlDialect extends CustomDialect {
//		
//		private final String schema;
//		
//		public MySqlDialect(String schema) {
//			super();
//			this.schema = schema;
//		}
//		
//		@Override
//		protected String postProcess(String sql) {
//			sql = sql.replace("`SCHEMA`", "`" + schema + "`")
//					  .replaceAll("__ISNOTNULL\\{(.*?)\\}", "($1 IS NOT NULL)");
////			System.out.println(sql);
//			return sql;
//		}
//		
//		protected <T> T newQueryBuilder(QueryType<?> queryType) {
//	        if (queryType instanceof DataQuery.Type) {
//	            switch ((DataQuery.Type) queryType) {
//	                case SELECT:
//	                    return (T) new MySelectQuery(this);
//                    default:
//	            }
//	        }
//	        if (queryType == BasicQuery.STRING) {
//	            return (T) new MyStringQuery();
//	        }
//	        return super.newQueryBuilder(queryType);
//	    }
//		
//		public static class MySelectQuery extends SelectQuery {
//
//			public MySelectQuery(AnsiSql dialect) {
//				super(dialect);
//			}
//			
//			@Override
//			public ResultSet execute(Connection connection) throws SQLException {
//				Timer.Job tt = new Timer.Job(getQueryString(), getArguments(0).toArray());
//				try {
//					return super.execute(connection);
//				} finally {
//					tt.done();
//				}
//			}
//		}
//		
//		public static class MyStringQuery extends AbstractQueryBuilder<StringQueryBuilder<?>> implements StringQueryBuilder<StringQueryBuilder<?>> {
//
//	        private final List<Object[]> batches = new ArrayList<>();
//	        private String query = null;
//	        
//	        public MyStringQuery() {
//	            super(0);
//	        }
//
//	        @Override
//	        protected StringQueryBuilder<?> addPart(DataQueryPart type, QueryPart part) {
//	            throw new UnsupportedOperationException();
//	        }
//
//	        @Override
//	        protected void buildQuery(StringBuilder sql) {
//	            sql.append(query);
//	        }
//
//	        @Override
//	        protected void collectArguments(List<Object> args, int batch) {
//	            args.addAll(Arrays.asList(batches.get(batch)));
//	        }
//
//	        @Override
//	        public QueryType<StringQueryBuilder<?>> getQueryType() {
//	            return BasicQuery.STRING;
//	        }
//
//	        @Override
//	        public StringQueryBuilder<?> query(String query) {
//	            this.query = query;
//	            return this;
//	        }
//
//	        @Override
//	        public StringQueryBuilder<?> batch(Object... values) {
//	            batches.add(values);
//	            return this;
//	        }
//
//	        @Override
//	        public int getBatchCount() {
//	            if (batches.size() == 1) return 0;
//	            return batches.size();
//	        }
//	        
//	        @Override
//			public ResultSet execute(Connection connection) throws SQLException {
//				Timer.Job tt = new Timer.Job(getQueryString(), getArguments(0).toArray());
//				try {
//					return super.execute(connection);
//				} finally {
//					tt.done();
//				}
//			}
//	    }
//		
//		
//		
//	};
}
