package de.hpi.accidit.testapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FibTest {

    int fib1 = 0;
    
    @Test
    public void test_fibR() {
    	fib1 = 1;
    	int f = fib(4);
    	assertEquals(5, f);
    }
    
    @Test
    public void test_fibI() {
    	fib1 = 1;
    	int f = fibonacci(4);
    	assertEquals(5, f);
    }
    
    private int fib(int n) {
    	if (n <= 1) {
    		return fib1;
    	} else {
    		return fib(n-1) + fib(n-2);
    	}
    }
	
	public int fibonacci(int x) {
		int f1 = fib1, f2 = 0;
		for (int i = 0; i < x; i++) {
			int f = f1;
			f1 += f2;
			f2 = f;
		}
		return f1;
	}
}
