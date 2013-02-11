package de.hpi.accidit.testapp;

import java.util.*;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ATest {
    
    public ATest() {
    }

    @Test
    public void test_get() {
        List<Integer> l = newList();
//        assertThat(l, hasSize(0));
        l.add(1);
        l.add(2);
        l.add(3);
        assertThat(l, hasSize(3));     
    }
    
    @Test
    public void test_array() {
        double[] ary = {0, 1, 2};
        if (ary[0] < 1) ary[1] = Math.PI;
        assertThat(ary[2], is(2.0));
    }

    public <T> List<T> newList() {
        return new ArrayList<>();
    }
    
    @Test
    public void test_iterator() {
        List<Object> list = Arrays.<Object>asList(1, 2, 3);
        for (Object o: list) {
            if (o == null) return;
        }
    }
    
    @Test
    public void test_shared_slots() {
        {
            int i = Math.abs(3);
            if (i < 0) return;
        }
        {
            List<Integer> l = newList();
            if (l != null) l.clear();
        }
    }
}
