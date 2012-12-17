package de.hpi.accidit.testapp;

import org.junit.Test;

public class STest {
    
    @Test
    public void test_sync() {
        final Object lock = new Object();
        lock.hashCode();
        Object foo = new Object();
        synchronized (lock) {
            foo.hashCode();
        }
    }
    
}
