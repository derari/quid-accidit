package de.hpi.accidit.testapp;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class XTest {
    
    X x = new X();
    
    public XTest() {
    }

    @Test
    public void test_convert_13() {
        String s = x.convert(13);
        assertThat(s, is("0xd"));
    }
}
