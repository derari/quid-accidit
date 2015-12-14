package de.hpi.accidit.eclipse.views.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkPool {
	
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				EXECUTOR.shutdownNow();
			}
		});
	}
	
	public static void execute(final Runnable r) {
		EXECUTOR.submit(r);
	}

}
