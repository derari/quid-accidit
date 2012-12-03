package de.hpi.accidit.trace;

import de.hpi.accidit.model.MethodDescriptor;
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
    
    private Out out = null;
    private Model model = null;
    private TraceSet ts = null;
    private int mTest1 = 0;
    private int fADouble = 0;
    private int fStaticText = 0;
    
    public ThreadTraceTest() {
    }
    
    @Before
    public void setUp() {
        out = spy(new PrintStreamOut());
        model = new Model(out);
        ts = new TraceSet(model);
        mTest1 = model.getType(ACLASS).getMethod("test1", DESC__V).getCodeId();
        fADouble = model.getType(ACLASS).getField("aDouble", "D").getCodeId();
        fStaticText = model.getType(ACLASS).getField("staticText", "[C").getCodeId();
        System.out.println();
    }
    
    @After
    public void tearDown() {
        System.out.println();
    }
    
    private static String desc(String r) {
        return "()" + r;
    }
    
    private static CallMatcher call(String m) {
        return new CallMatcher(m);
    }
    
    private static ExitMatcher exit() {
        return new ExitMatcher();
    }
    
    private static CallTrace anyCall() {
        return any();
    }
    
    private int m(String method, String desc) {
        return model.getType(ACLASS).getMethod(method, desc).getCodeId();
    }
    
    @Test
    public void test_begin() {
        ThreadTrace t = ts.begin(mTest1);
        t.enter(m("test1", desc("V")));
        t.returned(-1, null);
        t.end();
        verify(out).traceCall(argThat(call("test1")));
        verify(out).traceExit(anyCall(), argThat(exit().returningVoid()));
    }

    @Test
    public void test_enter_exit() {
        ThreadTrace t = ts.begin(mTest1);
        t.enter(m("test1", desc("V")));
        t.line(0);
        t.enter(m("super1", desc("V")));
        t.returned(0, null);
        t.line(1);
        t.enter(m("test2", desc("V")));
        t.returned(1, null);
        t.returned(-1, null);
        t.end();
        verify(out).traceCall(argThat(call("test1").depth(0)));
        verify(out).traceCall(argThat(call("super1").depth(1)));
        verify(out).traceCall(argThat(call("test2").depth(1)));
        verify(out, times(3)).traceExit(anyCall(), argThat(exit().returningVoid()));
    }
    
    @Test
    public void test_return_float() {
        ThreadTrace t = ts.begin(mTest1);
        float result = 3.1415f;
        long resultBits = Float.floatToIntBits(result);
        
        t.enter(m("testF1", desc("F")));
        t.returned(-1, result);
        t.end();
        
        verify(out).traceCall(argThat(call("testF1")));
        verify(out).traceExit(anyCall(), argThat(exit().returning(resultBits)));
    }
    
    @Test
    public void test_args() {
        MethodDescriptor test1 = model.getMethod(mTest1);
        test1.addVariable(0, "anInt", "int");
        test1.addVariable(1, "anArray", "java.lang.Object[]");
        
        ThreadTrace t = ts.begin(mTest1);
        t.enter(m("test1", desc("V")));
        t.argument(0, 17);
        t.argument(1, new Object[]{1});
        t.returned(5, null);
        t.end();
    }

    @Test
    public void test_vars() {
        MethodDescriptor test1 = model.getMethod(mTest1);
        test1.addVariable(0, "anInt", "int");
        test1.addVariable(1, "anArray", "java.lang.Object[]");
        
        ThreadTrace t = ts.begin(mTest1);
        t.enter(m("test1", desc("V")));
        t.variable(6, 0, 17);
        t.variable(7, 1, new Object[]{1});
        t.returned(8, null);
        t.end();
    }
    
    @Test
    public void test_fields() {
        AClass a = new AClass();
        ThreadTrace t = ts.begin(mTest1);
        t.enter(m("test1", desc("V")));
        t.put(a, 1.0d, fADouble, 6);
        t.get(a, 1.0d, fADouble, 7);
        t.get(null, AClass.staticText, fStaticText, 7);
        t.returned(9, null);
        t.end();
    }
    
    @Test
    public void test_arrays() {
        char[] array = AClass.staticText;
        ThreadTrace t = ts.begin(mTest1);
        t.enter(m("test1", desc("V")));
        t.aryPut(array, 'x', 0, 6);
        t.aryGet(array, 'x', 0, 7);
        t.returned(9, null);
        t.end();
    }
    
    @Test
    public void test_simple_ex() {
        ThreadTrace t = ts.begin(mTest1); 
        Throwable ex = new RuntimeException();
        
        t.enter(m("test1", desc("V")));
        t.thrown(6, ex);
        t.caught(8, ex);
        t.returned(9, null);
        t.end();
    }
    
    @Test
    public void test_fallthrough_ex() {
        Throwable ex = new RuntimeException();
        ThreadTrace t = ts.begin(mTest1);
        t.enter( m("test1", desc("V")));
        nested1(t, ex, true);
        t.caught(9, ex);
        t.returned(10, null);
        t.end();
        
        long exId = t.getObjectId(ex);
        verify(out, times(2)).traceExit(anyCall(), argThat(exit().failing(exId)));
    }

    @Test
    public void test_fall_out_ex() {
        Throwable ex = new RuntimeException();
        ThreadTrace t = ts.begin(mTest1);        
        nested1(t, ex, false);
        t.caught(9, ex);
        
        long exId = t.getObjectId(ex);
        verify(out, times(2)).traceExit(anyCall(), argThat(exit().failing(exId)));
        verify(out).end((ThreadTrace) any());
    }
    
    private void nested1(ThreadTrace t, Throwable ex, boolean traceLine) {
        if (traceLine) t.line(6);
        t.enter(m("test1", desc("V")));
        nested2(t, ex);
    }

    private void nested2(ThreadTrace t, Throwable ex) {
        t.line(7);
        t.enter(m("test1", desc("V")));
        t.thrown(8, ex);
    }
    
}
