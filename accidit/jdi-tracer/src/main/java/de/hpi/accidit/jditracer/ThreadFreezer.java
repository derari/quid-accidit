package de.hpi.accidit.jditracer;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Arian Treffer
 */
public class ThreadFreezer {
    
    static boolean ENABLE = true;
    
    private final Set<ThreadReference> threads = new HashSet<>();
    private final Set<ThreadReference> frozen = new HashSet<>();
    private final Set<ThreadReference> active = new HashSet<>();
    private final Map<ThreadReference, Integer> waitCounter = new HashMap<>();
    private final Map<ThreadReference, ThreadReference> waiting = new HashMap<>();
    private final Set<ThreadReference> waitingForAll = new HashSet<>();
    
    private final EventRequestManager erm;
    private final Map<ThreadReference, MonitorWaitRequest> waitRequests = new HashMap<>();
    private final Map<ThreadReference, MonitorWaitedRequest> waitedRequests = new HashMap<>();
    
    private int activationCount = 0;

    public ThreadFreezer(VirtualMachine vm) {
        erm = vm.eventRequestManager();
        for (ThreadReference t: vm.allThreads())
            threads.add(t);
    }
    
    public void threadStart(ThreadStartEvent e) {
        ThreadReference t = e.thread();
        if (!threads.add(t)) return;//throw new IllegalStateException(e.toString());
        if (!waitingForAll.isEmpty()) {
            waitCounter.put(t, waitingForAll.size());
        }
//        if (activationCount > 0) {
//            active.add(t);
//        }        
    }
    
    public void threadDeath(ThreadDeathEvent e) {
        ThreadReference t = e.thread();
        if (!threads.remove(t)) throw new IllegalStateException(e.toString());
        active.remove(t);
        frozen.remove(t);
        MonitorWaitRequest mwr = waitRequests.remove(t);
        if (mwr != null) erm.deleteEventRequest(mwr);
        MonitorWaitedRequest mdr = waitedRequests.remove(t);
        if (mdr != null) erm.deleteEventRequest(mdr);
    }
    
    public void makeActive(ThreadReference t) {
        if (!active.add(t)) throw new IllegalStateException(t.toString());
        if (activationCount > 0) {
            watch(t);
            if (frozen.remove(t)) t.resume();
        }
    }
    
    public void makeInactive(ThreadReference t) {
        if (!active.remove(t)) throw new IllegalStateException(t.toString());
        if (activationCount < 0) {
            unwatch(t);
            if (frozen.add(t)) t.suspend();
        }
    }
    
    public void activate() {
        if (!ENABLE) return;
        if (activationCount == 0) {
            for (ThreadReference t: threads) {
                if (!active.contains(t)) {
                    frozen.add(t);
                    t.suspend();
                }
            }
            for (ThreadReference t: active) {
                watch(t);
            }
        }
        activationCount++;
    }
    
    public void deactivate() {
        if (!ENABLE) return;
        activationCount--;
        if (activationCount == 0) {
            for (ThreadReference t: frozen) {
                t.resume();
            }
            frozen.clear();
            waiting.clear();
            waitCounter.clear();
            unwatchAll();
        }
    }
    
    private int getWaitCount(ThreadReference r) {
        Integer i = waitCounter.get(r);
        return i != null ? i : 0;
    }
    
    private int incWaitCount(ThreadReference r) {
        int v = getWaitCount(r) + 1;
        waitCounter.put(r, v);
        return v;
    }

    private int decWaitCount(ThreadReference r) {
        int v = getWaitCount(r) - 1;
        if (v < 0) throw new IllegalStateException(r.toString());
        waitCounter.put(r, v);
        return v;
    }
    
    private void watch(ThreadReference r) {
        MonitorWaitRequest mwr = waitRequests.get(r);
        if (mwr == null) {
            mwr = erm.createMonitorWaitRequest();
            mwr.addThreadFilter(r);
            waitRequests.put(r, mwr);
        }
        MonitorWaitedRequest mdr = waitedRequests.get(r);
        if (mdr == null) {
            mdr = erm.createMonitorWaitedRequest();
            mdr.addThreadFilter(r);
            waitedRequests.put(r, mdr);
        }
        mwr.enable();
        mdr.enable();
    }

    private void unwatch(ThreadReference r) {
        waitRequests.get(r).disable();
        waitedRequests.get(r).disable();
    }
    
    private void unwatchAll() {
        for (EventRequest r: waitRequests.values())
            r.disable();
        for (EventRequest r: waitedRequests.values())
            r.disable();
    }
    
    public void monitorWait(MonitorWaitEvent e) throws IncompatibleThreadStateException {
        if (activationCount > 0) {
            ThreadReference thisT = e.thread();
            ThreadReference otherT = e.monitor().owningThread();
            if (otherT != null && otherT != thisT) {
                waiting.put(thisT, otherT);
                incWaitCount(otherT);
                if (frozen.remove(otherT)) {
                    otherT.resume();
                    watch(otherT);
                }
            } else if (otherT == null) {
                if (!waitingForAll.add(thisT)) {
                    //throw new IllegalStateException(thisT.toString());
                }
                for (ThreadReference t: threads) {
                    incWaitCount(t);
                    if (frozen.remove(t)) {
                        t.resume();
                        watch(t);
                    } 
                }
            }
        }
    }

    public void monitorWaited(MonitorWaitedEvent e) {
        if (activationCount > 0) {
            ThreadReference thisT = e.thread();
            ThreadReference otherT = waiting.remove(thisT);
            if (otherT != null) {
                int w = decWaitCount(otherT);
                if (w == 0 && !active.contains(otherT) && frozen.add(otherT)) {
                    otherT.suspend();
                    unwatch(otherT);
                }
            } else if (otherT == null) {
                if (!waitingForAll.remove(thisT)) {
                    //throw new IllegalStateException(thisT.toString());
                }
                for (ThreadReference t: threads) {
                    int w = decWaitCount(t);
                    if (w == 0 && !active.contains(t) && frozen.add(t)) {
                        t.suspend();
                        unwatch(t);
                    }
                }
            }
        }
    }

}
