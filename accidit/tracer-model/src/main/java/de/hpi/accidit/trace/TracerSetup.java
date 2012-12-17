package de.hpi.accidit.trace;

/**
 *
 * @author Arian Treffer
 */
public class TracerSetup {
    
    private static TraceSet traceSet;
    private static boolean initialized = false;
    
    public static void setTraceSet(TraceSet ts) {
        if (initialized) throw new IllegalStateException("Tracer already initialized");
        traceSet = ts;
    }
    
    public static TraceSet getTraceSet() {
        initialized = true;
        return traceSet;
    }
    
}
