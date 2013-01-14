package de.hpi.accidit.trace;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assume.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;


/**
 *
 * @author derari
 */
public class WeakIdentityMapTest {
    
    public WeakIdentityMapTest() {
    }
    
    @SuppressWarnings("SleepWhileInLoop")
    private static boolean runGC() {
        WeakReference<Object> ref = new WeakReference<>(new Object());
        final int maxTry = 5;
        for (int i = 0; i < maxTry; i++) {
            System.gc();
            if (ref.get() == null) return true;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return false;
    }
    
    @Test
    public void test() {
        final WeakIdentityMap<Integer, Integer> instance = new WeakIdentityMap<>();
        
        final int len = 200;
        Integer[] keys = new Integer[len];
        for (int i = 0; i < len; i++) {
            Integer key = new Integer(0);
            instance.put(key, i);
            keys[i] = key;
        }
        
        assertThat(instance.size(), is(len));
        assertThat(instance.get(new Integer(0)), is(nullValue()));
        assertThat(instance.get(keys[1]), is(1));
        assertThat(instance.get(keys[99]), is(99));
        
        Integer key37 = keys[37];
        Arrays.fill(keys, null);
        
        assumeTrue(runGC());
        assertThat(instance.size(), is(1));
        assertThat(instance.get(key37), is(37));
    }
    
    @Test
    public void test_reinsert() {
        final WeakIdentityMap<Object, Integer> instance = new WeakIdentityMap<>();
        final Object key = new Object();
        instance.put(key, 1);
        instance.put(key, null);
        assertThat(instance.size(), is(0));
        instance.put(key, 1);
        instance.put(key, null);
        assertThat(instance.size(), is(0));
    }
    
 }
