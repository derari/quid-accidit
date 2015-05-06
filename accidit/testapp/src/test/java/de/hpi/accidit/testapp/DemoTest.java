package de.hpi.accidit.testapp;

import java.util.*;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class DemoTest {
    
    public DemoTest() {
    }

    @Test
    public void test_a() {
    	String[] airmail = {"airmail"};
    	steps(0,3);
    	
    	ShippingRequest r = new ShippingRequest();
    	r.id = 8;
    	r.notes = airmail;
    	r.status = "new";
    	steps(144);
    	
    	Object o = r.id;
    	r.destination = new Address();
    	steps(52);
    	
    	o = r.id;
    	o = r.notes;
    	steps(43);
    	r.notes = new String[]{"airmail", "fragile"};
    	steps(7);
    	r.status = "confirmed";
    	steps(217);
    	
    	o = r.id;
    	o = r.notes;
    	o = r.destination;
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
    
    static class ShippingRequest {
    	int id;
    	String[] notes;
    	String status;
    	Address destination;
    }
    
    static class Address {
    }
}
