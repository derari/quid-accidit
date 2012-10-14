package de.hpi.accidit.jditracer.model;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.MethodEntryEvent;
import de.hpi.accidit.jditracer.out.Out;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Arian Treffer
 */
public class Trace {
    
    private final Out out;
//    private final List<TestTrace> traces = new ArrayList<>();
    private final Model model;

    private int nextTestTraceId = 0;
    
    public Trace(Out out) {
        this.out = out;
        model = new Model(out);
    }

    public TestTrace createTestTrace(Method m, ThreadReference t) throws IncompatibleThreadStateException {
        TestTrace testTrace = new TestTrace(model, nextTestTraceId++, m, t);
//        traces.add(testTrace);
        return testTrace;
    }

    public void dump() {
//        for (TestTrace trace: traces)
//            trace.dump();
    }
    
}
