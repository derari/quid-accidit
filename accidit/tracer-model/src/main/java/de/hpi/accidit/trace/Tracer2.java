package de.hpi.accidit.trace;

import de.hpi.accidit.model.Model;
import java.util.concurrent.Callable;

/**
 *
 * @author Arian Treffer
 */
public class Tracer2 {

    private static Runnable initializer = null;
    private static boolean initialized = false;
    private static boolean failFastMode = false;
    
    public static void setup(Runnable initializer) {
        Tracer2.initializer = initializer;
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
        if (traceCount < 0) traceCount = 0;
    }
    

    public synchronized static void begin() {
        init();
        trace = false;
        ThreadTrace tt = traceSet.begin();
        if (failFastMode) {
            tt.failFastMode();
        }
        incTraceCount();
    }
    
    public synchronized static void end() {
        if (!trace) return;
        trace = false;
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
    
    public static void call(int methodId) {
        CALL.trace(methodId);
    }
    
    private static final EventI CALL = new EventI() {
        @Override
        protected void run(ThreadTrace t, int methodId) {
            t.call(methodId);
        }
    };
    
    public static void enter(int methodCode, Object inst) {
        ENTER.trace(methodCode, inst);
    }
    
    private static final EventIA ENTER = new EventIA() {
        @Override
        protected void run(ThreadTrace t, int methodCode, Object inst) {
            t.enter(methodCode, inst);
        }
    };
    
    public static void returnA(Object result, int methodCode, int line) {
        RETURN.trace(methodCode, line, result);
    }
    
    public static void returnV(int methodCode, int line) {
        RETURN.trace(methodCode, line, null);
    }
    
    public static void returnI(int result, int methodCode, int line) {
        RETURN.trace(methodCode, line, result);
    }
    
    public static void returnL(long result, int methodCode, int line) {
        RETURN.trace(methodCode, line, result);
    }
    
    public static void returnF(float result, int methodCode, int line) {
        RETURN.trace(methodCode, line, result);
    }
    
    public static void returnD(double result, int methodCode, int line) {
        RETURN.trace(methodCode, line, result);
    }
    
    private static final EventIIA RETURN = new EventIIA() {
        @Override
        protected void run(ThreadTrace t, int methodCode, int line, Object result) {
            t.returned(methodCode, line, result);
        }
    };
    
    public static void storeI(int value, int var, int line, int offset) {
        STORE.trace(line, offset, var, value);
    }
    
    public static void storeL(long value, int var, int line, int offset) {
        STORE.trace(line, offset, var, value);
    }
    
    public static void storeF(float value, int var, int line, int offset) {
        STORE.trace(line, offset, var, value);
    }
    
    public static void storeD(double value, int var, int line, int offset) {
        STORE.trace(line, offset, var, value);
    }
    
    public static void storeA(Object value, int var, int line, int offset) {
        STORE.trace(line, offset, var, value);
    }
    
    private static final EventIIIA STORE = new EventIIIA() {
        @Override
        protected void run(ThreadTrace t, int line, int offset, int var, Object value) {
            t.variable(line, offset, var, value);
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
    
    public static void aStoreB(Object array, int index, byte value, int line) {
        ASTORE.trace(index, line, array, value);
    }
    
    public static void aStoreC(Object array, int index, char value, int line) {
        ASTORE.trace(index, line, array, value);
    }
    
    public static void aStoreS(Object array, int index, short value, int line) {
        ASTORE.trace(index, line, array, value);
    }
    
    public static void aStoreI(Object array, int index, int value, int line) {
        ASTORE.trace(index, line, array, value);
    }
    
    public static void aStoreL(Object array, int index, long value, int line) {
        ASTORE.trace(index, line, array, value);
    }
    
    public static void aStoreF(Object array, int index, float value, int line) {
        ASTORE.trace(index, line, array, value);
    }
    
    public static void aStoreD(Object array, int index, double value, int line) {
        ASTORE.trace(index, line, array, value);
    }
    
    public static void aStoreA(Object array, int index, Object value, int line) {
        ASTORE.trace(index, line, array, value);
    }
    
    private static final EventIIAA ASTORE = new EventIIAA() {
        @Override
        protected void run(ThreadTrace t, int index, int line, Object array, Object value) {
            t.arrayStore(array, value, index, line);
        }
    };
    
    public static void aLoadB(Object array, int index, byte value, int line) {
        ALOAD.trace(index, line, array, value);
    }
    
    public static void aLoadC(Object array, int index, char value, int line) {
        ALOAD.trace(index, line, array, value);
    }
    
    public static void aLoadS(Object array, int index, short value, int line) {
        ALOAD.trace(index, line, array, value);
    }
    
    public static void aLoadI(Object array, int index, int value, int line) {
        ALOAD.trace(index, line, array, value);
    }
    
    public static void aLoadL(Object array, int index, long value, int line) {
        ALOAD.trace(index, line, array, value);
    }
    
    public static void aLoadF(Object array, int index, float value, int line) {
        ALOAD.trace(index, line, array, value);
    }
    
    public static void aLoadD(Object array, int index, double value, int line) {
        ALOAD.trace(index, line, array, value);
    }
    
    public static void aLoadA(Object array, int index, Object value, int line) {
        ALOAD.trace(index, line, array, value);
    }
    
    private static final EventIIAA ALOAD = new EventIIAA() {
        @Override
        protected void run(ThreadTrace t, int index, int line, Object array, Object value) {
            t.arrayLoad(array, value, index, line);
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
    
    /**
     * Synchronize on Tracer2.class before calling this!
     * @return Should be passed to resumeTrace
     */
    public static boolean pauseTrace() {
        boolean old = trace;
        trace = false;
        return old;
    }
    
    public static void resumeTrace(boolean t) {
        trace = t;
    }
    
    private static ThreadTrace threadTrace() {
        assert !trace : "disable tracing before calling thread trace";
        //trace = false;
        ThreadTrace tt = traceSet.get();
        //trace = true;
        return tt;
    }
    
    public static void dummyA(Object o) {
        System.out.println(o);
    }

    public static void failFastMode() {
        failFastMode = true;
    }
    
    static abstract class EventI {
        
        public final void trace(int i) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i);
                } finally {
                    trace = true;
                }
            }
        }
        
        protected abstract void run(ThreadTrace t, int i);
    }
    
    static abstract class EventIA {
        
        public final void trace(int i, Object a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, a);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, a);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, long a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, a);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, float a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, a);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, double a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, a);
                } finally {
                    trace = true;
                }
            };
        }
        
        protected abstract void run(ThreadTrace t, int i, Object a);
    }
    
    static abstract class EventIIA {
        
        public final void trace(int i, int j, Object a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, int a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, long a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, float a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, double a) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a);
                } finally {
                    trace = true;
                }
            }
        }
        
        protected abstract void run(ThreadTrace t, int i, int j, Object a);
    }
    
    static abstract class EventIIAA {
        
        public final void trace(int i, int j, Object a, Object b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, Object a, int b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, Object a, long b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, Object a, float b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, Object a, double b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        protected abstract void run(ThreadTrace t, int i, int j, Object a, Object b);
    }
    
        static abstract class EventIIIA {
        
        public final void trace(int i, int j, int a, Object b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, int a, int b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, int a, long b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, int a, float b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        public final void trace(int i, int j, int a, double b) {
            synchronized (Tracer2.class) {
                if (!trace) return;
                trace = false;
                try {
                    ThreadTrace t = threadTrace();
                    if (t == null) return;
                    run(t, i, j, a, b);
                } finally {
                    trace = true;
                }
            }
        }
        
        protected abstract void run(ThreadTrace t, int i, int j, int a, Object b);
    }
    
}
