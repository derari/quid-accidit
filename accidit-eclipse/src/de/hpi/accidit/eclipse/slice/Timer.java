package de.hpi.accidit.eclipse.slice;

import java.util.concurrent.atomic.AtomicLong;

public class Timer {

	private AtomicLong t = new AtomicLong(0);
	
	public void reset() {
		t.set(0);
	}
	
	public void enter() {
		t.addAndGet(-System.currentTimeMillis());
	}
	
	public void exit() {
		t.addAndGet(System.currentTimeMillis());
	}
	
	public long value() {
		return t.get();
	}
	
	@Override
	public String toString() {
		return "" + value();
	}
}
