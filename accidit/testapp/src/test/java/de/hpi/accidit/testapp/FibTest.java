package de.hpi.accidit.testapp;

import org.junit.Test;

public class FibTest {

	public int fibonacci(int x) {
		int f1 = 1, f2 = 0;
		for (int i = 0; i < x; i++) {
			int f = f1;
			f1 += f2;
			f2 = f;
			progress(i);
		}
		return f1;
	}
	
	
	@Test
	public void test_fib_5() {
		fibonacci(5);
	}
	
	private void progress(int i) {
		
	}
	
}
