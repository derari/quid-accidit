package de.hpi.accidit.testapp;

import static de.hpi.accidit.testapp.Geometry.coneVolume;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DemoTest {
	
	static {
		try {
			new DemoTest().test_geometry();
		} catch ( Throwable t) {
			
		}
		try {
			new DemoTest().test_initialize();
		} catch ( Throwable t) {
			
		}
	}
	
	private void test_initialize() {
		try {
			test_geometry();
		} catch ( Throwable t) {
			
		}
	}
    
    public DemoTest() {
    }

    @Test
    public void test_a() {
    	String[] airmail = {"airmail"};
    	steps(1770);
    	
    	ShippingRequest r = new ShippingRequest();
    	r.id = 8;
    	r.notes = airmail;
    	r.status = print("new");
    	steps(1440);
    	
    	Object o = r.status;
    	o = r.id;
        Address a = new Address();
    	r.destination = a;
    	steps(120);
        a.name = "Jon Doe";
        a.street = "Avenue Q 217";
        a.city = "New York";
        steps(400);
    	
    	o = r.id;
    	o = r.notes;
    	steps(430);
    	r.notes = new String[]{"airmail", print("fragile")};
    	steps(70);
    	r.status = print("confirmed");
    	steps(2170);
        
    	o = r.status;
    	o = r.id;
    	String[] notes = r.notes;
    	o = r.destination;
        steps(212);
        r.status = print("complete");
        print(a.name);
        print(a.street);
        print(a.city);
    	for (String s: airmail) print(s);
    	for (String s: notes) print(s);
    }
    
    static String print(String s) {
    	long l = 0;
    	for (int i = 0; i < s.length(); i++) {
    		char c = s.charAt(i);
    		l += c;
    	}
    	return s;
    }
    
    static void steps(int s) {
    	steps(s, s/17);
    }
    
    static void steps(int s, int o) {
    	if (s < 2*o) s = 2*o;
    	for (int i = 0; i < s-2; i++) {
    		if (i < o) {
    			new Object();
    			s--;
    		}
    	}
    }
    
    @Test
    public void test_volume() {
        double volume = new Cone().getVolume();
        System.out.println(volume);
        print("cone");
    }
    
    @Test
    public void test_geometry() {
    	Pyramid shape = new Pyramid(new Square(2), 6);
    	assertEquals(8.0, shape.getVolume(), 0.01);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
//    @Test
//    public void test_geometry2() {
//    	Square base = new Square(2);
//    	Pyramid shape = new Pyramid(base, 6);
//    	assertEquals(8, shape.getVolume(), 0.00001);
////    	double volume = shape.getVolume();
////    	System.out.println(volume);
//    }
    
    static {
        new Cone();
        new Geometry();
        new Pyramid(new Square(2), 6);
        try {
        	assertEquals(0.1d, 0.2d, 0.1d);
        } catch (Throwable t) {}
    }
    
    static class Cone {
        double getVolume() {
            return coneVolume(2, true, 6);
        }
    }
}
