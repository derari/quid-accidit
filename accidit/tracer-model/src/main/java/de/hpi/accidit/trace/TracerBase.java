package de.hpi.accidit.trace;

import de.hpi.accidit.model.Model;

public class TracerBase {

    protected static final TraceSet traceSet = TracerSetup.getTraceSet();
    public static final Model model = traceSet.getModel();
    protected static boolean trace = false;
    
    protected static ThreadTrace threadTrace() {
        assert !trace : "disable tracing before calling thread trace";
        //trace = false;
        ThreadTrace tt = traceSet.get();
        //trace = true;
        return tt;
    }
    
    public static void dummyA(Object o) {
        System.out.println(o);
    }
    
    static abstract class EventI {
        
        public final void trace(int i) {
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
        
        protected abstract void run(ThreadTrace t, int i, Object a);
    }
    
    static abstract class EventIIA {
        
        public final void trace(int i, int j, Object a) {
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
            synchronized (Tracer.class) {
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
