package de.hpi.accidit.jditracer;

import de.hpi.accidit.jditracer.model.Trace;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import de.hpi.accidit.jditracer.out.CsvOut;
import de.hpi.accidit.jditracer.out.Out;
import java.io.File;
import java.util.*;

/**
 *
 * @author Arian Treffer
 */
public class JdiTest {

    static EventRequest testCaseRun = null;
    static EventRequestManager erm;
    static GlobalFieldWatcher fieldWatcher;
    static Trace trace;
    static ThreadFreezer threadFreezer;
    
    static Map<ThreadReference, ThreadHandler> handlerMap = new HashMap<>();
    
    static ThreadHandler getHandler(ThreadReference thread) {
        ThreadHandler handler = handlerMap.get(thread);
        if (handler == null) {
            handler = new ThreadHandler(thread, fieldWatcher, trace);
            handlerMap.put(thread, handler);
        }
        return handler;
    }
    
    public static void main(String[] args) throws Exception {
        VirtualMachine vm = new VMAcquirer().connect(5000, 200, 100);
        vm.suspend();
        System.out.print("connected");
        erm = vm.eventRequestManager();
        fieldWatcher = new GlobalFieldWatcher(erm);
        
        ClassPrepareRequest cpr = erm.createClassPrepareRequest();
        //cpr.addClassExclusionFilter("java.*");
        cpr.addClassExclusionFilter("sun.*");
        cpr.addClassExclusionFilter("com.sun.*");
        cpr.enable();
        
//        MethodExitRequest mexr = erm.createMethodExitRequest();
//        mexr.addClassFilter("org.cthul.strings.format.RomansFormatTest");
//        mexr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
//        mexr.setEnabled(true);
        
        ThreadStartRequest tsr = erm.createThreadStartRequest();
        tsr.enable();
        ThreadDeathRequest tdr = erm.createThreadDeathRequest();
        tdr.enable();
        
        threadFreezer = new ThreadFreezer(vm);
        
        for (ReferenceType t: vm.allClasses()) {
            fieldWatcher.watchFields(t);
//            checkForInitialize(t);
            watchTestMethods(t);
        }
        
        EventQueue queue = vm.eventQueue();
        vm.resume();
        try (Out out = new CsvOut(new File("target/trace"))) {
            trace = new Trace(out);

            try {
                while (true) {
                    processEvents(queue);                    
                    if (ThreadHandler.triggerGC) {
                        ThreadHandler.triggerGC = false;
                        System.gc();
                    }
                }
            } catch (InterruptedException | VMDisconnectedException exc) {
            }
        } finally {
            HandlerThread.stop();
        }
    }

    private static void processEvents(EventQueue queue) throws IncompatibleThreadStateException, InterruptedException {
        EventSet eventSet = queue.remove();
        handleEvents(eventSet);
        eventSet.resume();
    }
    
    private static long lastMs = 0;
    private static String last = null;

    private static void handleEvents(EventSet eventSet) throws IncompatibleThreadStateException {
//        long ms = System.currentTimeMillis();
//        if (lastMs > 0 && (ms - lastMs) > 300) {
//            System.out.println(last);
//            System.out.println("  " + eventSet.eventIterator().nextEvent().toString());
//            System.out.println("  " + ((ms-lastMs)/100)/10.0);
//        }
//        lastMs = ms;
        for (Event e: eventSet) {
//            last = e.toString();
            if (e instanceof ClassPrepareEvent) {
                classPrepare((ClassPrepareEvent) e);
            } else if (testCaseRun != null && e.request() == testCaseRun) {
                watchAllTestMethods(e);
            } else if (e instanceof MonitorWaitedEvent) {
                threadFreezer.monitorWaited((MonitorWaitedEvent) e);
            } else if (e instanceof MonitorWaitEvent) {
                threadFreezer.monitorWait((MonitorWaitEvent) e);
            } else if (e instanceof LocatableEvent) {
                LocatableEvent le = (LocatableEvent) e;
                getHandler(le.thread()).handle(le);
            } else if (e instanceof ThreadStartEvent) {
                threadFreezer.threadStart((ThreadStartEvent) e);
            } else if (e instanceof ThreadDeathEvent) {
                threadFreezer.threadDeath((ThreadDeathEvent) e);
            }
        }
    }

    private static boolean initialized = false;
    
    private static void classPrepare(ClassPrepareEvent e) {
        fieldWatcher.watchFields(e.referenceType());
        //checkForInitialize(e.referenceType());
        watchTestMethods(e.referenceType());
    }

    private static int testMethods = 0;
    private static void watchTestMethods(ReferenceType t) {
        String name = t.name();
        if (name.endsWith("Test") || name.endsWith("Tests") || name.endsWith("TestSuite")) {
            if (name.contains("junit")) return;
            int i = 0;
            for (Method m: t.methods()) {
                if (!m.isAbstract() && !m.isNative() && m.name().startsWith("test")) {
                    Location l = m.locationOfCodeIndex(0);
                    BreakpointRequest r = erm.createBreakpointRequest(l);
                    r.enable();
                    i++;
                    testMethods++;
                    if (testMethods % 100 == 0) System.out.print("\nwatching " + testMethods + " test methods");
                }
            }
            if (i > 0) {
                System.out.print("\n" + t.name() + ": " + i + " tests");
            }
        }
    }
    
    private static void checkForInitialize(ReferenceType t) {
        if (!initialized) {
            String name = t.name();
            if (name.contains("junit") &&
                    (name.endsWith(".TestCase") || name.endsWith(".ParentRunner"))) {
                initialized = true;
                System.out.print("\n" + t.name() + " loaded.");
                
                Method run = null;
                for (Method m: t.visibleMethods()) {
                    if (m.name().equals("runTest")) {
                        run = m;
                    }
                }
                System.out.print("\nwaiting for " + run);
                Location l = run.locationOfCodeIndex(0);
                BreakpointRequest r = erm.createBreakpointRequest(l);
                testCaseRun = r;
                
//                testCaseRun = erm.createMethodEntryRequest();
//                testCaseRun.addClassFilter(t);
                
                testCaseRun.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                testCaseRun.enable();
            }
        }
    }
    
    private static void watchAllTestMethods(Event e) {
        if (e instanceof MethodEntryEvent) {
            MethodEntryEvent m = (MethodEntryEvent) e;
            if (!m.method().name().startsWith("run")) return;
            System.out.print("\nWatching methods... " + m.method());
            testCaseRun.disable();
            testCaseRun = null;
        } else {
            System.out.print("\nWatching test methods... ");
            testCaseRun.disable();
            testCaseRun = null;            
        }
        
        MethodEntryRequest menr = erm.createMethodEntryRequest();
        menr.addClassFilter("*Test");
        //menr.addClassFilter("*Tests");
        //menr.addClassFilter("*ClassTwip");
        menr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        menr.enable();

        menr = erm.createMethodEntryRequest();
        menr.addClassFilter("*Tests");
        //menr.addClassFilter("*Tests");
        //menr.addClassFilter("*ClassTwip");
        menr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        menr.enable();
        
        menr = erm.createMethodEntryRequest();
        menr.addClassFilter("*TestSuite");
        //menr.addClassFilter("*Tests");
        //menr.addClassFilter("*ClassTwip");
        menr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        menr.enable();
    }
    
}
