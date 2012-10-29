package de.hpi.accidit.jditracer;

import com.sun.jdi.Field;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.*;
import java.util.*;

/**
 * Manages field watch requests for all threads. If a field is disabled for
 * one thread, it will be managed on a by-thread basis.
 * 
 * Field requests are disabled if they trigger outside of a trace.
 * 
 * @author Arian Treffer
 */
public class GlobalFieldWatcher {
    
    private static final boolean THREAD_WATCHERS = false;
    private static final int FIELD_MOD = 10000;
    private static final int CLASS_MOD = 1000;
    private static final int T_FIELD_MOD = THREAD_WATCHERS ?
                (FIELD_MOD >= 1000 ? FIELD_MOD/10 : FIELD_MOD) :
                FIELD_MOD;
    private static final int T_I_FIELD_MOD = T_FIELD_MOD/10;
    
    private final EventRequestManager erm;
    private List<ReferenceType> preInitTypesToWatch = new ArrayList<>(1024);
    private List<Field> modInThreads = new ArrayList<>(1024);
    private List<Field> accInThreads = new ArrayList<>(1024);
    private List<T> threadFieldWatchers = new ArrayList<>();
    private int classCount = 0;
    private int fieldRequestCount = 0;
    
    private T globalT = THREAD_WATCHERS ? null : new T("all threads");
    
    public GlobalFieldWatcher(EventRequestManager erm) {
        this.erm = erm;
    }
    
    public synchronized void watchFields(ReferenceType type)  {
        if (preInitTypesToWatch != null) {
            preInitTypesToWatch.add(type);
        } else {
            createGlobalRequests(type);
        }
    }
    
    public synchronized void init() {
        if (preInitTypesToWatch == null) return;
        System.out.print("\nInitializing field watcher (" + preInitTypesToWatch.size() + " classes)");
        for (ReferenceType t: preInitTypesToWatch)
            createGlobalRequests(t);
        preInitTypesToWatch = null;
    }
    
    private void createGlobalRequests(ReferenceType type) {
        try {
            List<Field> fields = type.fields();
            classCount++;
            if (classCount % CLASS_MOD == 0) System.out.print("\nwatching " + classCount + " classes");

            if (globalT != null) {
                for (Field field: fields) {
                    globalT.add(field, true);
                    globalT.add(field, false);
                }
                return;
            }
            if (type.name().endsWith("CharsetDecoder")) {
                // for now, dont watch at all
//                // a bug? not having this causes the target vm to crash sometimes...
//                // charset decoder fields will always be disabled outside of tests
//                for (Field field: fields) {
//                    watchInThreads(field, true);
//                    watchInThreads(field, false);
//                }
                return;
            }
            
            for (Field field : fields) {
                ModificationWatchpointRequest mwReq =
                        erm.createModificationWatchpointRequest(field);
                initWatchpointRequest(mwReq);

                AccessWatchpointRequest awReq =
                        erm.createAccessWatchpointRequest(field);
                initWatchpointRequest(awReq);
                fieldRequestCount += 2;
                if (fieldRequestCount % FIELD_MOD == 0) System.out.printf("%n%d global field requests", fieldRequestCount);
            }
        } catch (ObjectCollectedException e) { }
    }
    
    private void initWatchpointRequest(WatchpointRequest wReq) {
        wReq.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        wReq.enable();
    }
    
    private synchronized void watchInThreads(WatchpointRequest wReq) {
        if (wReq.getProperty("inThreads") != null) return;
        wReq.disable();
        wReq.putProperty("inThreads", Boolean.TRUE);
        Field f = wReq.field();
        erm.deleteEventRequest(wReq);
//        delFieldReqCount++;
//        if (delFieldReqCount % FIELD_MOD == 0) System.out.println("watching " + delFieldReqCount + " field requests in threads");
        watchInThreads(f, wReq instanceof ModificationWatchpointRequest);
    }

    private void watchInThreads(Field f, boolean mod) {
        for (T t: threadFieldWatchers)
            t.add(f, mod);
        if (mod) {
            modInThreads.add(f);
        } else {
            accInThreads.add(f);
        }
    }
    
    public synchronized T getT(ThreadReference thread) {
        if (globalT != null) return globalT;
        
        for (T t: threadFieldWatchers) {
            if (t.thread.equals(thread)) return t;
        }
        T t = new T(thread);
        threadFieldWatchers.add(t);
        t.addAll(modInThreads, true);
        t.addAll(accInThreads, false);
        return t;
    }

    public class T {
        
        private final ThreadReference thread;  
        private final String name;
        private List<Field> preInitWatchMod = new ArrayList<>(1024);
        private List<Field> preInitWatchAcc = new ArrayList<>(1024);
        private final RequestStrategy strategy;
        private int enableCount = 0;
        private int fieldRequestCount = 0;
        
        public T(ThreadReference thread) {
            this.thread = thread;
            this.name = thread.name();
            strategy = new OnDemandReset(this.name);
        }
        
        public T(String thread) {
            this.thread = null;
            this.name = thread;
            strategy = new OnDemandReset(this.name);
        }
        
        private synchronized void addAll(Collection<? extends Field> threadLocalFields, boolean mod) {
            if (preInitWatchMod != null) {
                if (mod) {
                    preInitWatchMod.addAll(threadLocalFields);
                } else {
                    preInitWatchAcc.addAll(threadLocalFields);
                }
            } else {
                for (Field f: threadLocalFields)
                    createRequests(f, mod);
            }
        }

        private void add(Field f, boolean mod) {
            if (preInitWatchMod != null) {
                if (mod) {
                    preInitWatchMod.add(f);
                } else {
                    preInitWatchAcc.add(f);
                }
            } else {
                createRequests(f, mod);
            }
        }
        
        private synchronized void init() {
            GlobalFieldWatcher.this.init();
            System.out.print("\nInitializing field watcher (" + name + ", " + (preInitWatchMod.size() + preInitWatchAcc.size()) + " field requests)");
            for (Field f: preInitWatchMod)
                createRequests(f, true);
            for (Field f: preInitWatchAcc)
                createRequests(f, false);
            preInitWatchMod = null;
            preInitWatchAcc = null;
        }
    
        private void createRequests(Field field, boolean mod) {
            WatchpointRequest wReq = mod ? 
                    erm.createModificationWatchpointRequest(field) :
                    erm.createAccessWatchpointRequest(field);
            initWatchpointRequest(wReq);
            fieldRequestCount++;
            if (fieldRequestCount % T_FIELD_MOD == 0) System.out.printf("%n%s: %d field requests", name, fieldRequestCount);
        }

        private void initWatchpointRequest(WatchpointRequest wReq) {
            wReq.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            if (thread != null) wReq.addThreadFilter(thread);
            wReq.putProperty("threadWatcher", this);
            strategy.add(wReq);
        }

        public synchronized void enable() {
            if (enableCount == 0) {
                if (preInitWatchMod != null) init();
                strategy.enable();
            }
            enableCount++;
        }

        public synchronized void disable() {
            enableCount--;
            if (enableCount == 0) {
                strategy.disable();
            }
        }

        /**
        * Some fields are used outside of tests. Disable their requests to
        * avoid unwanted events.
        */
        public synchronized void autoDisable(WatchpointRequest req) {
            if (enableCount == 0) {
                Object watcher = req.getProperty("threadWatcher");
                if (watcher == null) {
                    watchInThreads(req);
                } else {
                    if (watcher != this) throw new AssertionError(req);
                    strategy.autoDisable(req);
                }
            }
        }

        public void resetAutoDisable() {
            strategy.reset();
        }

        public void beginIgnore() {
            strategy.beginIgnore();
        }

        public void endIgnore() {
            strategy.endIgnore();
        }

        public void ignore(WatchpointRequest req) {
            Object watcher = req.getProperty("threadWatcher");
            if (watcher == null) {
                watchInThreads(req);
            } else {
                if (watcher != this) throw new AssertionError(req);
                strategy.ignore(req);
            }
        }

    }
    
//    private boolean arrayTest = true;

    private static interface RequestStrategy {
        public void add(WatchpointRequest req);
        public void enable();
        public void disable();
        public void autoDisable(WatchpointRequest req);
        public void reset();
        
        public void ignore(WatchpointRequest req);
        public void beginIgnore();
        public void endIgnore();
    }
    
//    /**
//     * Disabling all watchpoint requests outside of tests greatly improves
//     * the overall execution time. 
//     * However, toggling several thousand requests also takes quite a while.
//     */
//    private static class ToggleAll implements RequestStrategy {
//        private final List<WatchpointRequest> requests = new ArrayList<>(1024*8);
//        private boolean enabled = false;
//        @Override
//        public void add(WatchpointRequest req) {
//            requests.add(req);
//            if (enabled) req.enable();
//        }
//
//        @Override
//        public void enable() {
//            enabled = true;
//            for (WatchpointRequest req: requests)
//                req.enable();
//        }
//
//        @Override
//        public void disable() {
//            enabled = false;
//            for (WatchpointRequest req: requests)
//                req.disable();
//        }
//
//        @Override
//        public void autoDisable(WatchpointRequest req) {
//        }
//
//        @Override
//        public void reset() {
//        }
//
//        @Override
//        public String toString() {
//            return requests.size() + " field reqeuests";
//        }
//        
//    }
    
    /**
     * To speed up toggling, only those requests are disabled that actually
     * match outside of tests.
     */
    private static class OnDemand implements RequestStrategy {
        protected final String name;
        protected List<WatchpointRequest> toBeEnabled = new ArrayList<>(1024*4);
        protected List<WatchpointRequest> autoDisable = new ArrayList<>(1024*2);
        protected List<WatchpointRequest> alwaysDisable = new ArrayList<>(2);
        protected List<WatchpointRequest> ignore = new ArrayList<>(1024);        
        protected boolean enabled = false;
        protected int lastAutoDisableSize = 0;
        protected int autoCounter = 0;
        protected int total = 0;
        protected boolean ignoreEnabled = false;
        
        protected Integer autoDisableToken = 1;

        public OnDemand(String name) {
            this.name = name;
        }
        
        @Override
        public void add(WatchpointRequest req) {
            total++;
            //if (total % 10000 == 0) System.out.println("watching " + total + " fields");
            if (req.field().declaringType().name().endsWith("CharsetDecoder")) {
                // a bug? not having this causes the target vm to crash sometimes...
                alwaysDisable.add(req);
                if (enabled) req.enable();
                return;
            }
            if (enabled) req.enable();
            else toBeEnabled.add(req);
        }

        @Override
        public void enable() {
            enabled = true;
            if (!toBeEnabled.isEmpty()) {
                for (WatchpointRequest req: toBeEnabled)
                    req.enable();
                toBeEnabled = new ArrayList<>(1024); // quicker than clear()
            }
            if (autoCounter > 0) {
                System.out.printf("%n%s: auto-disable +%d: %d/%d", name, autoCounter, autoDisable.size(), total);
//                if (lastAutoDisableSize > 0) {
//                    int stop = lastAutoDisableSize + Math.min(autoCounter, 5);
//                    for (int i = lastAutoDisableSize; i < stop; i++) {
//                        WatchpointRequest r = autoDisable.get(i);
//                        boolean set = r instanceof ModificationWatchpointRequest;
//                        System.out.println("  " + (set? "<" : ">") + " " + r.field());
//                    }
//                }
                lastAutoDisableSize = autoDisable.size();
                autoCounter = 0;
            }
            for (WatchpointRequest req: autoDisable)
                req.enable();
            for (WatchpointRequest req: alwaysDisable)
                req.enable();
        }

        @Override
        public void disable() {
            enabled = false;
            for (WatchpointRequest req: autoDisable)
                req.disable();
            for (WatchpointRequest req: alwaysDisable)
                req.disable();
        }

        @Override
        public void autoDisable(WatchpointRequest req) {
            if (req.getProperty("autoDisableToken") != autoDisableToken) {
                req.disable();
                req.putProperty("autoDisableToken", autoDisableToken);
                autoCounter++;
                autoDisable.add(req);
            }
        }

        @Override
        public void reset() {
        }

        @Override
        public String toString() {
            return autoDisable.size() + " fields auto-disabled";
        }

        @Override
        public void ignore(WatchpointRequest req) {
            if (ignoreEnabled && req.isEnabled()) {
                req.disable();
                ignore.add(req);
                if (ignore.size() % T_I_FIELD_MOD == 0) System.out.printf("%n%s: %d requests on ignore", name, ignore.size());
            }
        }

        @Override
        public void beginIgnore() {
            if (!ignoreEnabled) {
                ignoreEnabled = true;
                for (WatchpointRequest r: ignore)
                    r.disable();
            }
        }

        @Override
        public void endIgnore() {
            if (ignoreEnabled) {
                ignoreEnabled = false;
                if (!ignore.isEmpty()) {
                    for (WatchpointRequest r: ignore)
                        r.enable();
                    ignore = new ArrayList<>(100);
                }
            }
        }
    }
    
    /**
     * Clearing the auto-disable list after a new test class is run
     * removes request for fields that were only used in the set-up of the
     * previous test class or the static set-up of the current, 
     * and don't have to be disabled any more.
     */
    private static class OnDemandReset extends OnDemand {

        public OnDemandReset(String name) {
            super(name);
        }

        @Override
        public void reset() {
            if (!enabled) throw new IllegalStateException("x");
            System.out.printf("%n%s: auto-disable reset", name);
            int min = lastAutoDisableSize;
            int size = 1024;
            while (size < min) size *= 2;
            lastAutoDisableSize = 0;
            autoDisable = new ArrayList<>(size);
            autoDisableToken = autoDisableToken + 1;
        }
        
    }
    
//    private static final int THREAD_COUNT = 4;
//    
//    private final List<ModificationWatchpointRequest>[] tRequests = new List[THREAD_COUNT];
//    private final Object signal = new Object();
//    
//    private final Thread[] threads = new Thread[4];
//    {
//        for (int i = 0; i < THREAD_COUNT; i++) {
//            tRequests[i] = new ArrayList<>(1024);
//            threads[i] = new SwitchThread(tRequests[i]);
//        }
//    }
//    
//    private class SwitchThread extends Thread {
//        private final List<ModificationWatchpointRequest> requests;
//
//        public SwitchThread(List<ModificationWatchpointRequest> requests) {
//            this.requests = requests;
//        }
//
//        @Override
//        public void run() {
//            try {
//                synchronized (signal) {
//                    signal.wait();
//                }
//                if (enableCount > 0) {
//                    for (ModificationWatchpointRequest r: requests)
//                        r.enable();
//                } else {
//                    for (ModificationWatchpointRequest r: requests)
//                        r.disable();                    
//                }
//            } catch (InterruptedException ex) {}
//        }
//    }
    
//    public synchronized void arrayTest(ArrayType type) {
//        if (!arrayTest) return;
//        arrayTest = true;
//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + type);
//        for (Method m: type.methods()) {
//            System.out.println("!!! " + m);
//        }
//        List<Field> fields = type.visibleFields();
//        for (Field field : fields) {
//            System.out.println("!!! " + field);
//            ModificationWatchpointRequest req =
//                     erm.createModificationWatchpointRequest(field);
//            req.setSuspendPolicy(EventRequest.SUSPEND_NONE);
//            requests.add(req);
//        }
//        System.out.println(".");
//        throw new Error("exit");
//    }

}
