package de.hpi.accidit.eclipse.views.util;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Timer {
	
	private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);
			
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				EXECUTOR.shutdownNow();
			}
		});
	}
	
	private static final AtomicInteger count = new AtomicInteger(0);
	
	public static class Job {
		
		private final int n = count.getAndIncrement();
		private final String query;
		private final Object[] args;
		private final long start = System.currentTimeMillis();
		private volatile boolean done = false;
		private boolean first = true;
		
		public Job(String query, Object[] args) {
			this.query = query;
			this.args = args;
			initial();
		}
		
		private void initial() {
			EXECUTOR.schedule(this::update, 3, TimeUnit.SECONDS);
		}
		
		private void update() {
			if (done) return;
			if (first) {
				System.out.println(n + ": " + query);
				System.out.println(n + ": " + Arrays.toString(args));
				first = false;
			}
			System.out.println(n + ": " + (System.currentTimeMillis() - start)/1000 + "s");
			EXECUTOR.schedule(this::update, 1, TimeUnit.SECONDS);
		}
		
		public void done() {
			done = true;
		}
	}
}
