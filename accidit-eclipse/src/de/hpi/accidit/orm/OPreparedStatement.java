package de.hpi.accidit.orm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Future;

import de.hpi.accidit.orm.util.OFutureBase;

public class OPreparedStatement {

	private final OConnection cnn;
	private final String sql;
	
	public OPreparedStatement(OConnection cnn, String sql) {
		this.cnn = cnn;
		this.sql = sql;
	}

	public OFuture<ResultSet> submit(Object[] args) {
		QueryRun query = new QueryRun(this, args);
		query.setFutureDelegate(cnn.submit(query));
		return query.result;
	}
	
	public ResultSet run(Object[] args) throws SQLException {
		PreparedStatement stmt = cnn.preparedStatement(sql);
		synchronized (stmt) {
			fillArgs(stmt, args);
			return stmt.executeQuery();			
		}
	}
	
	private static class QueryRun implements Runnable {
		
		private final OPreparedStatement stmt;
		private final Object[] args;
		
		final QueryResult result = new QueryResult();
		
		public QueryRun(OPreparedStatement stmt, Object[] args) {
			this.stmt = stmt;
			this.args = args;
		}
		
		void setFutureDelegate(Future<?> f) {
			result.cancelDelegate = f;
		}

		@Override
		public void run() {
			final ResultSet rs;
			try {
				rs = stmt.run(args);
			} catch (Throwable t) {
				result.setException(t);
				if (t instanceof Error) {
					throw (Error) t;
				}
				if (t instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				return;
			}
			result.setValue(rs);
		}
	}
	
	private static class QueryResult extends OFutureBase<ResultSet> {

		Future<?> cancelDelegate;
		
		public QueryResult() {
			super(null);
		}
		
		@Override
		protected Future<?> getCancelDelegate() {
			return cancelDelegate;
		}
		
		@Override
		protected void setException(Throwable exception) {
			super.setException(exception);
		}

		@Override
		protected void setValue(ResultSet value) {
			super.setValue(value);
		}
		
	}
	
	private static void fillArgs(PreparedStatement stmt, Object[] args) throws SQLException {
		for (int i = 0; i < args.length; i++) {
			setArg(stmt, i+1, args[i]);
		}
	}

	private static void setArg(PreparedStatement stmt, int i, Object arg) throws SQLException {
		if (arg == null) {
			stmt.setObject(i, null);
		} else if (arg instanceof String) {
			stmt.setString(i, (String) arg);
		} else if (arg instanceof Number) {
			Number n = (Number) arg;
			if (n instanceof Integer) {
				stmt.setInt(i, n.intValue());
			} else if (n instanceof Long) {
				stmt.setLong(i, n.longValue());
			} else {
				throw new IllegalArgumentException(
						arg.getClass().getCanonicalName() + " " +
						String.valueOf(arg));
			}
		} else {
			throw new IllegalArgumentException(
					arg.getClass().getCanonicalName() + " " +
					String.valueOf(arg));
		}
	}
	
}
