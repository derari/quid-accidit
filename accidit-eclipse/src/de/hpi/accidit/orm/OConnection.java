package de.hpi.accidit.orm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.hpi.accidit.orm.dsl.Select;

public class OConnection implements AutoCloseable {

	private final Connection connection;
	private final ExecutorService executor = Executors.newFixedThreadPool(3);
	
	public OConnection(Connection connection) {
		this.connection = connection;
		runningExecs.put(executor, true);
	}
	
	public OPreparedStatement prepare(String sql) throws SQLException {
		return new OPreparedStatement(this, connection.prepareStatement(sql));
	}
	
	/**
	 * Called by OPreparedStatement 
	 */
	protected Future<?> submit(Runnable queryCommand) {
		return executor.submit(queryCommand);
	}

	@Override
	public void close() throws Exception {
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
