package de.hpi.accidit.testapp;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ATest {
    
    public ATest() {
    }

    @Test
    public void test_get() {
        List<Integer> l = newList();
        l.add(1);
        l.add(2);
        l.add(3);
        assertThat(l, hasSize(3));     
    }

    public <T> List<T> newList() {
        return new ArrayList<>();
    }
}
