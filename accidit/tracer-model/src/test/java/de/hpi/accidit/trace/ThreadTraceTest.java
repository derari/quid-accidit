package de.hpi.accidit.trace;

import de.hpi.accidit.trace.program.AClass;
import de.hpi.accidit.model.Model;
import de.hpi.accidit.out.Out;
import de.hpi.accidit.out.PrintStreamOut;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Arian Treffer
 */
public class ThreadTraceTest {
    
    private static final String ACLASS = AClass.class.getCanonicalName();
    private static final String DESC__V = "()V";
    
    private TestOut out = null;
    private Model model = null;
    private TraceSet ts = null;
    private int mTest1 = 0;
    
    public ThreadTraceTest() {
    }
    
    @Before
    public void setUp() {
        out = spy(new TestOut());
        model = new Model(out);
        ts = new TraceSet(model);
        mTest1 = model.getType(ACLASS).getMethod("test1", DESC__V).getCodeId();
        System.out.println();
    }
    
    @After
    public void tearDown() {
        System.out.println();
    }
    
    private static String desc(String r) {
        return "()" + r;
    }
    
    private static InvocationMatcher inv(String m) {
        return new InvocationMatcher(m);
    }
    
    private void enter(ThreadTrace t, int line, String method, String desc) {
        int mId = model.getType(ACLASS).getMethod(method, desc).getCodeId();
        t.enter(line, mId);
    }
    
    @Test
    public void test_begin() {
        ThreadTrace t = ts.begin(mTest1);
        enter(t, -1, "test1", desc("V"));
        t.returned(null);
        t.end();
        verify(out).invocation(argThat(inv("test1").returningVoid()));
    }

    @Test
    public void test_enter_exit() {
        ThreadTrace t = ts.begin(mTest1);
        enter(t, -1, "test1", desc("V"));
        enter(t, 0, "super1", desc("V"));
        t.returned(null);
        enter(t, 1, "test2", desc("V"));
        t.returned(null);
        t.returned(null);
        t.end();
        verify(out).invocation(argThat(inv("test1").depth(0).returningVoid()));
        verify(out).invocation(argThat(inv("super1").depth(1).returningVoid()));
        verify(out).invocation(argThat(inv("test2").depth(1).returningVoid()));
    }
    
    @Test
    public void test_return_float() {
        ThreadTrace t = ts.begin(mTest1);
        float result = 3.1415f;
        long resultBits = Float.floatToIntBits(result);
        
        enter(t, -1, "testF1", desc("F"));
        t.returned(result);
        t.end();
        
        verify(out).invocation(argThat(inv("testF1x").returning(resultBits)));
    }
    
    static class TestOut extends PrintStreamOut {

        @Override
        public void invocation(InvocationTrace invocation) {
            super.invocation(invocation);
        }
        
    }
    
}
