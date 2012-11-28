package de.hpi.accidit.trace;

import java.lang.ref.*;

public class WeakIdentityMap<K, V> {
    
    private static final MappedWeakReference GUARD = new MappedWeakReference();
    
    private static final int INITIAL_CAPICITY = 64;
    private static final float LOAD_FACTOR = 3/4f;
    
    private final float loadFactor = LOAD_FACTOR;
    private int capacity = INITIAL_CAPICITY;
    private int mask = -1;
    private int size = 0;

    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
    private MappedWeakReference[] keys;
    private Object[] values;

    public WeakIdentityMap() {
        initMap();
    }
    
    private void initMap() {
        keys = new MappedWeakReference[capacity];
        values = new Object[capacity];
        mask = capacity-1;
    }
    
    public int size() {
        cleanUp();
        return size;
    }
    
    public synchronized V put(K key, V value) {
        cleanUp();
        if (value == null) return remove(key);
        final int hash = hash(key);
        int index = find(hash, key);
        if (index < 0) {
            size++; 
            ensureCapacity();
            index = insertAt(hash);
            keys[index] = new MappedWeakReference(hash, key, refQueue);
        }
        V oldValue = (V) values[index];
        values[index] = value;
        return oldValue;
    }
    
    public synchronized V get(Object key) {
        final int hash = hash(key);
        int index = find(hash, key);
        if (index < 0) return null;
        return (V) values[index];
    }
    
    public synchronized V remove(Object key) {
        final int hash = hash(key);
        int index = find(hash, key);
        if (index < 0) return null;
        return removeAt(index);
    }
    
    private V remove(MappedWeakReference key) {
        int index = find(key);
        if (index < 0) return null;
        return removeAt(index);
    }
    
    private void ensureCapacity() {
        while (size > capacity*loadFactor) grow();
    }
    
    private void grow() {
        MappedWeakReference[] oldKeys = keys;
        Object[] oldValues = values;
        int oldCap = capacity;
        capacity *= 2;
        initMap();
        for (int i = 0; i < oldCap; i++) {
            MappedWeakReference key = oldKeys[i];
            if (key != null && key != GUARD) {
                int index = insertAt(key.hash);
                keys[index] = key;
                values[index] = oldValues[i];
            }
        }
    }
    
    private int find(int hash, Object key) {
        int index = index(hash);
        final int maxTry = capacity;
        for (int i = 0; i < maxTry; i++) {
            MappedWeakReference keyRef = keys[index];
            if (keyRef == null) {
                return -1;
            }
            Object obj = keyRef.get();
            if (obj == null) {
                removeAt(index);
            } else if (obj == key) {
                return index;
            }
            index = nextIndex(index);
        }
        return -1;
    }
    
    private int find(MappedWeakReference key) {
        int index = index(key.hash);
        final int maxTry = capacity;
        for (int i = 0; i < maxTry; i++) {
            MappedWeakReference keyRef = keys[index];
            if (keyRef == null) {
                return -1;
            } else if (keyRef == key) {
                return index;
            }
            index = nextIndex(index);
        }
        return -1;
    }
    
    private int insertAt(int hash) {
        int index = index(hash);
        final int maxTry = capacity;
        for (int i = 0; i < maxTry; i++) {
            MappedWeakReference keyRef = keys[index];
            if (keyRef == null || keyRef == GUARD) {
                return index;
            } else {
                index = nextIndex(index);
            }
        }
        // there *has* to be capacity left, find it!
        return insertAt(hash+1);
    }
    
    private void cleanUp() {
        Reference<?> r = refQueue.poll();
        while (r != null) {
            remove((MappedWeakReference) r);
            r = refQueue.poll();
        }
    }
    
    private int index(int hash) {
        return hash & mask;
    }
    
    private int nextIndex(int index) {
        return (index + 2) & mask;
    }

    private static int hash(Object key) {
        return System.identityHashCode(key);
    }

    private V removeAt(int index) {
        size--; 
        V oldValue = (V) values[index];
        keys[index] = GUARD;
        values[index] = null;
        return oldValue;
    }
    
    private static class MappedWeakReference extends WeakReference<Object> {

        private final int hash;
        
        public MappedWeakReference() {
            super(new Object());
            this.hash = 0;
        }

        public MappedWeakReference(int hash, Object referent, ReferenceQueue<? super Object> q) {
            super(referent, q);
            this.hash = hash;
        }
        
    }
    
}
