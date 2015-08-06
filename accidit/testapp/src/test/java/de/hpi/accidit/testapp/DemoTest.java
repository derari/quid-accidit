package de.hpi.accidit.testapp;

import org.junit.Test;
import static de.hpi.accidit.testapp.Geometry.*;

public class DemoTest {
    
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
    
    static {
        new Cone();
        new Geometry();
    }
    
    static class Cone {
        double getVolume() {
            return coneVolume(2, true, 6);
        }
    }
}
