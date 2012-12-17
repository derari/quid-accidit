package de.hpi.accidit.trace.program;

public class AClass extends Super implements Iface {
    
    public double aDouble;
    
    public static char[] staticText = new char[]{'o', 'k'};
    
    public void test1() {
    }
    
    public void test2() {
    }

    @Override
    public void super1() {
        super.super1();
    }

    @Override
    public void iface1() {
    }
    
    public float testF1() {
        return 1.2f;
    }
    
}
