package de.hpi.accidit.orm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.hpi.accidit.orm.dsl.Select;
import de.hpi.accidit.orm.util.OFutureBase;

public class OConnection implements AutoCloseable {

	private final Connection connection;
	private final ExecutorService executor = Executors.newFixedThreadPool(3);
	
	public OConnection(Connection connection) {
		this.connection = connection;
		runningExecs.put(executor, true);
	}
	
	public OPreparedStatement prepare(String sql) throws SQLException {
		return new OPreparedStatement(this, sql);
	}
	
	/* OPreparedStatement */ PreparedStatement preparedStatement(String sql) throws SQLException {
		return connection.prepareStatement(sql);
	}
	
	/**
	 * Called by OPreparedStatement 
	 */
	protected Future<?> submit(Runnable queryCommand) {
		return executor.submit(queryCommand);
	}
	
	public <P, R> OFuture<R> submit(OFutureAction<P, R> action, P arg) {
		ActionResult<P, R> result = new ActionResult<P, R>(action, arg);
		result.cancelDelegate = submit(result);
		return result;
	}

	@Override
	public void close() throws SQLException {
		try {
			boolean terminated = false;
			try {
				terminated = executor.awaitTermination(100, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (!terminated) {
				executor.shutdownNow();
			}
		} finally {
			connection.close();
			runningExecs.remove(executor);
		}
	}
	
	private static class ActionResult<P, R>
					extends OFutureBase<R>
					implements Runnable {

		Future<?> cancelDelegate = null;
		private final OFutureAction<P, R> action;
		private final P param;
		
		public ActionResult(OFutureAction<P, R> action, P param) {
			super(null);
			this.action = action;
			this.param = param;
		}

		@Override
		protected Future<?> getCancelDelegate() {
			return cancelDelegate;
		}
		
		@Override
		public void run() {
			try {
				setValue(action.call(param));
			} catch (Throwable e) {
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				setException(e);
			}
		}		
	}
	
	// Executor management =========================================================
	
	private static final ConcurrentMap<ExecutorService, Boolean> runningExecs = new ConcurrentHashMap<>();
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				for (ExecutorService e: runningExecs.keySet()) {
					try {
						e.shutdownNow();
					} catch (Throwable t) {
						t.printStackTrace(System.err);
					}
				}
			}
		});
	}
	
	// DSL ==========================================================================
	
	private Select selectAll = null;
	
	public Select select() {
		if (selectAll == null) {
			selectAll = new Select(this);
		}
		return selectAll;
	}
	
	public Select select(String... fields) {
		return new Select(this, fields);
	}
	
}
