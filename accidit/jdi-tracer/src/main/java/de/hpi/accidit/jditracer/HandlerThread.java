package de.hpi.accidit.jditracer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author Arian Treffer
 */
public class HandlerThread {
    
    private static BlockingQueue<Runnable> queue;
    
    private static final Thread instance = new Thread() {

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    queue.take().run();
                }
            } catch (InterruptedException _) { 
            } finally {
                queue = null;
            }
        }
    };
    
    static {
        queue = new ArrayBlockingQueue<>(16);
        instance.start();
    }
    
    public static void run(Runnable r) {
        try {
            if (queue == null) throw new IllegalStateException("stopped");
            queue.put(r);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
//        r.run();
    }
    
    public static void stop() {
        instance.interrupt();
    }
    
}