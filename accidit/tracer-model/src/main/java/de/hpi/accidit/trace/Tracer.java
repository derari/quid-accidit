package de.hpi.accidit.trace;

import de.hpi.accidit.model.FieldDescriptor;
import de.hpi.accidit.model.MethodDescriptor;
import de.hpi.accidit.model.Model;
import de.hpi.accidit.out.PrintStreamOut;

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
    
    private static int t = 0;    
    private static boolean trace = false;    
    private static ThreadTrace tracer;

    public static void begin(int methodCode) {
        init();
        if (t == 0) {
            assert tracer == null : "Should not trace yet";
            lastLine = -1;
            depth = 0;
            tracer = traceSet.begin(methodCode);
            MethodDescriptor m = model.getMethod(methodCode);
            String s = m.toString() + " ";
            String s2 = "================================================================================".substring(s.length());
            System.out.print(s);
            System.out.println(s2);
        }
        t++;
        trace = true;
    }
    
    public static void end() {
        t--;
        trace = t > 0;
        if (!trace) {
            tracer.end();
            tracer = null;
        }
    }

    private static int depth = 0;
    
    private static void indent() {
        final int indent = depth;
        for (int i = 0; i < indent; i++) {
            System.out.print(" ");
        }
    }
    
    private static int lastLine = -1;
    
    public static void line(int line) {
        LINE.trace(line);
    }
    
    private static final EventI LINE = new EventI() {
        @Override
        protected void run(int line) {
            lastLine = line;
        }
    };
    
    public static void enter(int methodCode) {
        ENTER.trace(methodCode);
    }
    
    private static final EventI ENTER = new EventI() {
        @Override
        protected void run(int methodCode) {
            tracer.enter(lastLine, methodCode);
            MethodDescriptor m = model.getMethod(methodCode);
            indent();
            System.out.printf("%d: %s%n", lastLine, m);
            depth++;
        }
    };
    
    public static void returnA(Object result, int methodCode) {
        RETURN.trace(methodCode, result);
    }
    
    public static void returnV(int methodCode) {
        RETURN.trace(methodCode, null);
    }
    
    public static void returnI(int result, int methodCode) {
        RETURN.trace(methodCode, result);
    }
    
    public static void returnL(long result, int methodCode) {
        RETURN.trace(methodCode, result);
    }
    
    public static void returnF(float result, int methodCode) {
        RETURN.trace(methodCode, result);
    }
    
    public static void returnD(double result, int methodCode) {
        RETURN.trace(methodCode, result);
    }
    
    private static final EventIA RETURN = new EventIA() {
        @Override
        protected void run(int methodCode, Object result) {
            MethodDescriptor m = model.getMethod(methodCode);
            indent();
            System.out.println("< " + m.getName() + " " + String.valueOf(result));
            depth--;
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
        protected void run(int var, int line, Object o) {
            indent();
            System.out.println(line + ": " + var + " <- " + String.valueOf(o));
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
        protected void run(int i, Object o) {
            indent();
            System.out.println(i + " := " + String.valueOf(o));
        }
    };
    
    public static void putI(int value, int field, int line) {
        PUT.trace(field, line, null, value);
    }
    
    public static void putL(long value, int field, int line) {
        PUT.trace(field, line, null, value);
    }
    
    public static void putA(Object value, int field, int line) {
        PUT.trace(field, line, null, value);
    }
    
    public static void putA(Object obj, Object value, int field, int line) {
        PUT.trace(field, line, obj, value);
    }
    
    private static final EventIIAA PUT = new EventIIAA() {
        @Override
        protected void run(int field, int line, Object obj, Object value) {
            indent();
            FieldDescriptor f = model.getField(field);
            System.out.println(line  + ": " + (obj == null ? "" : obj+".") + f + " <- " + value);
        }
    };
    
    public static void getI(int value, int field, int line) {
        GET.trace(field, line, null, value);
    }
    
    public static void getL(long value, int field, int line) {
        GET.trace(field, line, null, value);
    }
    
    public static void getA(Object value, int field, int line) {
        GET.trace(field, line, null, value);
    }
    
    public static void getA(Object obj, Object value, int field, int line) {
        GET.trace(field, line, obj, value);
    }
    
    public static void thrown(Object ex, int line) {
        THROW.trace(line, ex);
    }
    
    private static final EventIA THROW = new EventIA() {
        @Override
        protected void run(int line, Object ex) {
            indent();
            System.out.printf("%d: !! %s%n", line ,ex);
        }
    };
    
    public static void caught(Object ex, int line) {
        CATCH.trace(line, ex);
    }
    
    private static final EventIA CATCH = new EventIA() {
        @Override
        protected void run(int line, Object ex) {
            indent();
            System.out.printf("%d: {{ %s%n", line ,ex);
        }
    };
    
    private static final EventIIAA GET = new EventIIAA() {
        @Override
        protected void run(int field, int line, Object obj, Object value) {
            indent();
            FieldDescriptor f = model.getField(field);
            System.out.println(line  + ": " + (obj == null ? "" : obj+".") + f + " -> " + value);
        }
    };
    
    public static boolean pauseTracing() {
        boolean old = trace;
        trace = false;
        return old;
    }
    
    public static void resumeTracing(boolean old) {
        trace = old;
    }
    
    static abstract class EventI {
        
        public final void trace(int i) {
            if (!trace) return;
            trace = false;
            run(i);
            trace = true;
        }
        
        protected abstract void run(int i);
    }
    
    static abstract class EventIA {
        
        public final void trace(int i, Object a) {
            if (!trace) return;
            trace = false;
            run(i, a);
            trace = true;
        }
        
        protected abstract void run(int i, Object o);
    }
    
    static abstract class EventIIA {
        
        public final void trace(int i, int j, Object a) {
            if (!trace) return;
            trace = false;
            run(i, j, a);
            trace = true;
        }
        
        protected abstract void run(int i, int j, Object o);
    }
    
    static abstract class EventIIAA {
        
        public final void trace(int i, int j, Object a, Object b) {
            if (!trace) return;
            trace = false;
            run(i, j, a, b);
            trace = true;
        }
        
        protected abstract void run(int i, int j, Object a, Object b);
    }
    
    public static void dummyA(Object o) {
        System.out.println("dummyA " + o);
    }

    public static void dummyI(int o) {
        System.out.println("dummyI " + o);
    }
    
}
