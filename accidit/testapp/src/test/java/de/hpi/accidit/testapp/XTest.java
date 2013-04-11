package de.hpi.accidit.testapp;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class XTest {
    
    X x = new X();
    
    public XTest() {
        Assert.assertEquals("1", "1");
    }

    @Test
    public void test_convert_13() {
        String s = x.convert(13);
        assertThat(s, is("0xd"));
    }
    
    private static String s1 = "ab";
    @SuppressWarnings("RedundantStringConstructorCall")
    private static String s2 = new String(s1);
    
    @Test
    public void test_str_eq() {
        Assert.assertEquals(s1, s2);
    }
    
}
