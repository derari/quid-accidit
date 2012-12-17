package de.hpi.accidit.testapp;

import de.hpi.accidit.asmtracer.TestAt;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ASimpleTest {
    
    public static long globalStrCount = 0;

    public String last;
    private long aLong = 1337;

    public ASimpleTest() {
        last = null;
    }
    
    public String str(int i) {
        globalStrCount++;
        String s = stringValue(i);
        last = s;
        return s == null? "" : s;
    }
    
    public String basicTest() {
        int j = 17;
        String s = str(j);
        if (last == null) globalStrCount--;
        return s;
    }
    
    private long getLong() {
        return new Access().get();
    }

    private static String stringValue(int i) {
        return String.valueOf(i);
    }

    public int exTest() {
        int i = 0;
        i += catch1();
        i += catch2() * 4;
        return i;
    }
    
    private void throw1() {
        throw new RuntimeException("nope");
    }
    
    private int catch1() {
        try {
            throw1();
            return 1;
        } catch (RuntimeException e) {
            return 2;
        }
    }
    
    private boolean isPositive(short s) {
        return Math.abs(s) > 0;
    }
    
    private int throw2() {
        RuntimeException re;
        try {
            if (isPositive((short) -3)) throw new RuntimeException("nope");
            return 1;
        } catch (RuntimeException e) {
            re = e;
        }
        if (re != null) throw re;
        return 2;
    }
    
    private int fallThrough() {
        return throw2();
    }
    
    private int catch2() {
        try {
            return fallThrough();
        } catch (RuntimeException e) {
            return 3;
        }
    }
    
    public String newTest() {
        return new ASimpleTest().str(13);
    }
    
    public static int[][] container = new int[][]{{0,0}};

    public void arrayTest() {
        int[][] data = new int[][]{ null, {}, new int[]{2, 0} };
        data[2] = new int[3];
        container[0][1]++;
        boolean[] flags = new boolean[]{true};
        flags.hashCode();
    }
    
    public void testTest() { 
        // test method with begin/end trace
        List<Integer> l = newList();
        l.add(1);
        l.add(2);
        l.add(3);
        assertThat(l, hasSize(3));
        assertEquals("my pi", Math.PI, 3.1415, 0.01);
    }

    public <T> List<T> newList() {
        return new ArrayList<>();
    }
    
    static Object hasSize(int i) {
        return i;
    }
    
    static void assertThat(Object o, Object p) {}
    
    public Object nestedTest() {
        return getLong();
    }
    
    public long nested2Test() {
        new PrintStream(new ByteArrayOutputStream(1024)).println(this);
        return aLong;
    }

    @Override
    public String toString() {
        return "" + aLong++;
    }
    
    static public void assertEquals(String message, double expected,
                    double actual, double delta) {
            if (Double.compare(expected, actual) == 0)
                    return;
            if (!(Math.abs(expected - actual) <= delta))
                    failNotEquals(message, new Double(expected), new Double(actual));
    }

    private static void failNotEquals(String message, Double aDouble, Double aDouble0) {
        throw new UnsupportedOperationException();
    }

    @TestAt
    class Access {

        public Access() {
            int i = 0;
            aLong += i;
        }

        public long get() {
            return aLong;
        }
    }
    
}
