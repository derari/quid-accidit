package de.hpi.accidit.trace;

import de.hpi.accidit.model.Model;
import java.util.concurrent.Callable;

/**
 *
 * @author Arian Treffer
 */
public class TraceSet {
    
    private final Model model;
    private final Traces traces = new Traces();
    private final OnEnd onEnd = new OnEnd();
    private int nextTraceId = 0;

    public TraceSet(Model model) {
        this.model = model;
    }
    
    public ThreadTrace begin() {
        ThreadTrace trace = traces.get();
        if (trace == null) {
            synchronized (this) {
                trace = new ThreadTrace(model, nextTraceId++, onEnd);
                traces.set(trace);
            }
        }
        return trace;
    }

    public Model getModel() {
        return model;
    }
    
    /** Call only when tracing is disabled! */
    public ThreadTrace get() {
        return traces.get();
    }
    
    private class Traces extends ThreadLocal<ThreadTrace> {
    }
    
    private class OnEnd implements ThreadTrace.EndCallback {
        @Override
        public void ended(ThreadTrace trace) {
            traces.remove();
        }
    }
}
