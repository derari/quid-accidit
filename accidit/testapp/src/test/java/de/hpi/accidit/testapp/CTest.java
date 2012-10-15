package de.hpi.accidit.testapp;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class CTest {
    
    public CTest() {
    }

    @Test
    public void test_get() {
        String s = new C().get('h', 'e', 'l', 'l', 'o');
        assertThat(s, is("hello"));
    }
}
