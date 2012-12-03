package de.hpi.accidit.trace;

import de.hpi.accidit.model.Model;
import java.util.concurrent.Callable;

/**
 *
 * @author Arian Treffer
 */
public class Tracer {

    private static Runnable initializer = null;
    private static boolean initialized = false;
    
    public static void setup(Runnable initializer) {
        Tracer.initializer = initializer;
    }
    
    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        trace = false;
        
        try {
            if (initializer != null) {
                initializer.run();
                initializer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static final TraceSet traceSet = TracerSetup.getTraceSet();
    public static final Model model = traceSet.getModel();
    
    private static int traceCount = 0;    
    private static boolean trace = false;
    
    public static boolean isTracing() {
        return trace;
    }
    
    private static synchronized void incTraceCount() {
        traceCount++;
        trace = true;
    }
    
    private static synchronized void decTraceCount() {
        traceCount--;
        trace = traceCount > 0;
    }
    

    public static void begin(int methodCode) {
        init();
        traceSet.begin(methodCode);
        incTraceCount();
    }
    
    public static void end() {
        if (!trace) return;
        ThreadTrace t = threadTrace();
        if (t == null) return;
        decTraceCount();
        t.end();
    }

    public static void line(int line) {
        LINE.trace(line);
    }
    
    private static final EventI LINE = new EventI() {
        @Override
        protected void run(ThreadTrace t, int line) {
            t.line(line);
        }
    };
    
    public static void enter(int methodCode) {
        ENTER.trace(methodCode);
    }
    
    private static final EventI ENTER = new EventI() {
        @Override
        protected void run(ThreadTrace t, int methodCode) {
            t.enter(methodCode);
        }
    };
    
    public static void returnA(Object result, int methodCode) {
        RETURN.trace(methodCode, result);
    }
    
    public static void returnV(int line) {
        RETURN.trace(line, null);
    }
    
    public static void returnI(int result, int line) {
        RETURN.trace(line, result);
    }
    
    public static void returnL(long result, int line) {
        RETURN.trace(line, result);
    }
    
    public static void returnF(float result, int line) {
        RETURN.trace(line, result);
    }
    
    public static void returnD(double result, int line) {
        RETURN.trace(line, result);
    }
    
    private static final EventIA RETURN = new EventIA() {
        @Override
        protected void run(ThreadTrace t, int line, Object result) {
            t.returned(line, result);
        }
    };
    
    public static void storeI(int value, int var, int line) {
        STORE.trace(var, line, value);
    }
    
    public static void storeL(long value, int var, int line) {
        STORE.trace(var, line, value);
    }
    
    public static void storeF(float value, int var, int line) {
        STORE.trace(var, line, value);
    }
    
    public static void storeD(double value, int var, int line) {
        STORE.trace(var, line, value);
    }
    
    public static void storeA(Object value, int var, int line) {
        STORE.trace(var, line, value);
    }
    
    private static final EventIIA STORE = new EventIIA() {
        @Override
        protected void run(ThreadTrace t, int var, int line, Object value) {
            t.variable(line, var, value);
        }
    };
    
    public static void argI(int value, int var) {
        ARG.trace(var, value);
    }
    
    public static void argL(long value, int var) {
        ARG.trace(var, value);
    }
    
    public static void argF(float value, int var) {
        ARG.trace(var, value);
    }
    
    public static void argD(double value, int var) {
        ARG.trace(var, value);
    }
    
    public static void argA(Object value, int var) {
        ARG.trace(var, value);
    }
    
    private static final EventIA ARG = new EventIA() {
        @Override
        protected void run(ThreadTrace t, int argId, Object value) {
            t.argument(argId, value);
        }
    };
    
    public static void putI(int value, int field, int line) {
        PUT.trace(field, line, null, value);
    }
    
    public static void putL(long value, int field, int line) {
        PUT.trace(field, line, null, value);
    }
    
    public static void putF(float value, int field, int line) {
        PUT.trace(field, line, null, value);
    }
    
    public static void putD(double value, int field, int line) {
        PUT.trace(field, line, null, value);
    }
    
    public static void putA(Object value, int field, int line) {
        PUT.trace(field, line, null, value);
    }
    
    public static void putA(Object instance, Object value, int field, int line) {
        PUT.trace(field, line, instance, value);
    }
    
    public static void putI(Object instance, int value, int field, int line) {
        PUT.trace(field, line, instance, value);
    }
    
    public static void putL(Object instance, long value, int field, int line) {
        PUT.trace(field, line, instance, value);
    }
    
    public static void putF(Object instance, float value, int field, int line) {
        PUT.trace(field, line, instance, value);
    }
    
    public static void putD(Object instance, double value, int field, int line) {
        PUT.trace(field, line, instance, value);
    }
    
    private static final EventIIAA PUT = new EventIIAA() {
        @Override
        protected void run(ThreadTrace t, int field, int line, Object obj, Object value) {
            t.put(obj, value, field, line);
        }
    };
    
    public static void getI(int value, int field, int line) {
        GET.trace(field, line, null, value);
    }
    
    public static void getL(long value, int field, int line) {
        GET.trace(field, line, null, value);
    }
    
    public static void getF(float value, int field, int line) {
        GET.trace(field, line, null, value);
    }
    
    public static void getD(double value, int field, int line) {
        GET.trace(field, line, null, value);
    }
    
    public static void getA(Object value, int field, int line) {
        GET.trace(field, line, null, value);
    }
    
    public static void getA(Object instance, Object value, int field, int line) {
        GET.trace(field, line, instance, value);
    }
    
    public static void getI(Object instance, int value, int field, int line) {
        GET.trace(field, line, instance, value);
    }
    
    public static void getL(Object instance, long value, int field, int line) {
        GET.trace(field, line, instance, value);
    }
    
    public static void getF(Object instance, float value, int field, int line) {
        GET.trace(field, line, instance, value);
    }
    
    public static void getD(Object instance, double value, int field, int line) {
        GET.trace(field, line, instance, value);
    }
    
    private static final EventIIAA GET = new EventIIAA() {
        @Override
        protected void run(ThreadTrace t, int field, int line, Object obj, Object value) {
            t.get(obj, value, field, line);
        }
    };
    
    public static void thrown(Object ex, int line) {
        THROW.trace(line, ex);
    }
    
    private static final EventIA THROW = new EventIA() {
        @Override
        protected void run(ThreadTrace t, int line, Object ex) {
            t.thrown(line, ex);
        }
    };
    
    public static void caught(Object ex, int line) {
        CATCH.trace(line, ex);
    }
    
    private static final EventIA CATCH = new EventIA() {
        @Override
        protected void run(ThreadTrace t, int line, Object ex) {
            t.caught(line, ex);
        }
    };
    
    public static synchronized <T> T noTrace(Callable<T> call) throws Exception {
        if (!trace) return call.call();
        trace = false;
        try {
            return call.call();
        } finally {
            trace = true;
        }
    }
    
    private synchronized static ThreadTrace threadTrace() {
        assert trace : "should only be called when tracing";
        trace = false;
        ThreadTrace tt = traceSet.get();
        trace = true;
        return tt;
    }
    
    static abstract class EventI {
        
        public final void trace(int i) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i);
        }
        
        protected abstract void run(ThreadTrace t, int i);
    }
    
    static abstract class EventIA {
        
        public final void trace(int i, Object a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, a);
        }
        
        public final void trace(int i, int a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, a);
        }
        
        public final void trace(int i, long a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, a);
        }
        
        public final void trace(int i, float a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, a);
        }
        
        public final void trace(int i, double a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, a);
        }
        
        protected abstract void run(ThreadTrace t, int i, Object a);
    }
    
    static abstract class EventIIA {
        
        public final void trace(int i, int j, Object a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a);
        }
        
        public final void trace(int i, int j, int a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a);
        }
        
        public final void trace(int i, int j, long a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a);
        }
        
        public final void trace(int i, int j, float a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a);
        }
        
        public final void trace(int i, int j, double a) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a);
        }
        
        protected abstract void run(ThreadTrace t, int i, int j, Object a);
    }
    
    static abstract class EventIIAA {
        
        public final void trace(int i, int j, Object a, Object b) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a, b);
        }
        
        public final void trace(int i, int j, Object a, int b) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a, b);
        }
        
        public final void trace(int i, int j, Object a, long b) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a, b);
        }
        
        public final void trace(int i, int j, Object a, float b) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a, b);
        }
        
        public final void trace(int i, int j, Object a, double b) {
            if (!trace) return;
            ThreadTrace t = threadTrace();
            if (t == null) return;
            run(t, i, j, a, b);
        }
        
        protected abstract void run(ThreadTrace t, int i, int j, Object a, Object b);
    }
    
}
