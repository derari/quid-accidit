package de.hpi.accidit.asmtracer;

public class AClass {
    
    public static long globalStrCount = 0;
    public String last;

    public AClass() {
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
    
    private int throw2() {
        RuntimeException re = null;
        try {
            if (Math.abs(-3.0) > 0) throw new RuntimeException("nope");
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
        return new AClass().str(13);
    }

}
