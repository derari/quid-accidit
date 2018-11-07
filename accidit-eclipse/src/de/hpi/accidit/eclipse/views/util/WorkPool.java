package de.hpi.accidit.eclipse.views.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkPool {
	
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(8);
	private static final ExecutorService PRIO_EXECUTOR = Executors.newFixedThreadPool(2);
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				EXECUTOR.shutdownNow();
				PRIO_EXECUTOR.shutdownNow();
			}
		});
	}
	
	public static void execute(final Runnable r) {
		EXECUTOR.submit(r);
	}
	
	public static void executePriority(final Runnable r) {
		PRIO_EXECUTOR.submit(r);
	}

}
