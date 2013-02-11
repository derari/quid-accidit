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
    private static final ClassLoader cl = AClass.class.getClassLoader();
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
        mTest1 = model.getType(ACLASS, cl).getMethod("test1", DESC__V).getCodeId();
        fADouble = model.getType(ACLASS, cl).getField("aDouble", "D").getCodeId();
        fStaticText = model.getType(ACLASS, cl).getField("staticText", "[C").getCodeId();
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
        return model.getType(ACLASS, cl).getMethod(method, desc).getCodeId();
    }
    
    @Test
    public void test_begin() {
        AClass a = new AClass();
        ThreadTrace t = ts.begin();
        t.enter(m("test1", desc("V")), a);
        t.returned(-1, -1, null);
        t.end();
        verify(out).traceCall(argThat(call("test1")));
        verify(out).traceExit(anyCall(), argThat(exit().returningVoid()));
    }

    @Test
    public void test_enter_exit() {
        AClass a = new AClass();
        ThreadTrace t = ts.begin();
        t.enter(m("test1", desc("V")), a);
        t.line(0); 
        t.call(m("super1", desc("V")));
        t.enter(m("super1", desc("V")), a);
        t.returned(-1, 0, null);
        t.line(1);
        t.call(m("test2", desc("V")));
        t.enter(m("test2", desc("V")), a);
        t.returned(-1, 1, null);
        t.returned(-1, -1, null);
        t.end();
        verify(out).traceCall(argThat(call("test1").depth(0)));
        verify(out).traceCall(argThat(call("super1").depth(1)));
        verify(out).traceCall(argThat(call("test2").depth(1)));
        verify(out, times(3)).traceExit(anyCall(), argThat(exit().returningVoid()));
    }
    
    @Test
    public void test_return_float() {
        AClass a = new AClass();
        ThreadTrace t = ts.begin();
        float result = 3.1415f;
        long resultBits = Float.floatToIntBits(result);
        
        t.enter(m("testF1", desc("F")), a);
        t.returned(-1, -1, result);
        t.end();
        
        verify(out).traceCall(argThat(call("testF1")));
        verify(out).traceExit(anyCall(), argThat(exit().returning(resultBits)));
    }
    
    @Test
    public void test_args() {
        AClass a = new AClass();
        MethodDescriptor test1 = model.getMethod(mTest1);
        test1.addVariable(0, 0, "anInt", "int");
        test1.addVariable(1, 0, "anArray", "java.lang.Object[]");
        
        ThreadTrace t = ts.begin();
        t.enter(m("test1", desc("V")), a);
        t.argument(0, 17);
        t.argument(1, new Object[]{1});
        t.returned(-1, 5, null);
        t.end();
    }

    @Test
    public void test_vars() {
        AClass a = new AClass();
        MethodDescriptor test1 = model.getMethod(mTest1);
        test1.addVariable(0, 0, "anInt", "int");
        test1.addVariable(1, 0, "anArray", "java.lang.Object[]");
        
        ThreadTrace t = ts.begin();
        t.enter(m("test1", desc("V")), a);
        t.variable(6, 0, 0, 17);
        t.variable(7, 0, 1, new Object[]{1});
        t.returned(-1, 8, null);
        t.end();
    }
    
    @Test
    public void test_fields() {
        AClass a = new AClass();
        ThreadTrace t = ts.begin();
        t.enter(m("test1", desc("V")), a);
        t.put(a, 1.0d, fADouble, 6);
        t.get(a, 1.0d, fADouble, 7);
        t.get(null, AClass.staticText, fStaticText, 7);
        t.returned(-1, 9, null);
        t.end();
    }
    
    @Test
    public void test_arrays() {
        AClass a = new AClass();
        char[] array = AClass.staticText;
        ThreadTrace t = ts.begin();
        t.enter(m("test1", desc("V")), a);
        t.arrayStore(array, 'x', 0, 6);
        t.arrayLoad(array, 'x', 0, 7);
        t.returned(-1, 9, null);
        t.end();
    }
    
    @Test
    public void test_simple_ex() {
        AClass a = new AClass();
        ThreadTrace t = ts.begin(); 
        Throwable ex = new RuntimeException();
        
        t.enter(m("test1", desc("V")), a);
        t.thrown(6, ex);
        t.caught(8, ex);
        t.returned(-1, 9, null);
        t.end();
    }
    
    @Test
    public void test_fallthrough_ex() {
        AClass a = new AClass();
        Throwable ex = new RuntimeException();
        ThreadTrace t = ts.begin();
        t.enter( m("test1", desc("V")), a);
        nested1(t, a, ex, true);
        t.caught(9, ex);
        t.returned(-1, 10, null);
        t.end();
        
        long exId = t.getObjectId(ex);
        verify(out, times(2)).traceExit(anyCall(), argThat(exit().failing(exId)));
    }

    @Test
    public void test_fall_out_ex() {
        AClass a = new AClass();
        Throwable ex = new RuntimeException();
        ThreadTrace t = ts.begin();        
        nested1(t, a, ex, false);
        t.caught(9, ex);
        
        long exId = t.getObjectId(ex);
        verify(out, times(2)).traceExit(anyCall(), argThat(exit().failing(exId)));
        verify(out).end((ThreadTrace) any());
    }
    
    private void nested1(ThreadTrace t, AClass a, Throwable ex, boolean traceCall) {
        if (traceCall) {
            t.line(6);
            t.call(m("test1", desc("V")));
        }
        t.enter(m("test1", desc("V")), a);
        nested2(t, a, ex);
    }

    private void nested2(ThreadTrace t, AClass a, Throwable ex) {
        t.line(7);
        t.call(m("test1", desc("V")));
        t.enter(m("test1", desc("V")), a);
        t.thrown(8, ex);
    }
    
}
