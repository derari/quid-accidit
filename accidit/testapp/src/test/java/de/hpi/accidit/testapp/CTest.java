package de.hpi.accidit.testapp;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class CTest {
    
    public CTest() {
    }

    @Test
    public void test_get() {
        String s = c().get('h', 'e', 'l', 'l', 'o');
        assertThat(s, is("hello"));
    }
    
    @Test
    public void test_split() {
        int i = "abcdef".split(",").length;
        assertThat(i, is(1));
    }

    public C c() {
        return new C();
    }
    
}
