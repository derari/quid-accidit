package de.hpi.accidit.trace;

/**
 *
 * @author Arian Treffer
 */
public class Step {
    
    protected long step = -1;
    
    public final long next() {
        return ++step;
    }

    public long current() {
        return step;
    }
    
}
