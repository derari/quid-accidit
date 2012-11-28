package de.hpi.accidit.testapp;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ETest {
    
    private boolean e2 = false;
    private boolean re = false;
    
    public ETest() {
    }
    
    @Test
    public void dummy() {
        assertThat("somethin is", true);
    }
//    
//    public void e() throws Exception {
//        throw new Exception("E");
//    }
//    
//    public void e2() throws Exception {
//        try {
//            e();
//            assertThat("not reached", false);
//        } catch (Exception e) {
//            e2 = true;
//            throw e;
//        }
//    }
//    
//    public void re() {
//        try {
//            e2();
//            assertThat("not reached", false);
//        } catch (Exception e) {
//            re = true;
//            throw new RuntimeException("RE", e);
//        }
//    }
//
//    @Test
//    public void test_re() {
//        try {
//            re();
//            assertThat("not reached", false);
//        } catch (Exception e) {
//            assertThat("e2", e2);
//            assertThat("re", re);
//        }
//    }
//    
//    @Test(expected=Exception.class)
//    public void test_e() throws Exception {
//        e();
//    }
//    
}
