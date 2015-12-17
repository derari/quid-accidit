package de.hpi.accidit.eclipse.slice;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Utility class for lazily caching objects.
 * @param <K>
 * @param <V>
 */
public class Cache<K, V> {
	
	private final ConcurrentMap<K, Reference<V>> map = new ConcurrentHashMap<>();
	private final Function<K, V> function;

	public Cache() {
		this(k -> { throw new UnsupportedOperationException("value(K) not implemented");});
	}
	
	public Cache(Function<K, V> function) {
		super();
		this.function = function;
	}

	public V get(K key) {
		Reference<V> ref = map.get(key);
		V v = ref == null ? null : ref.get();
		if (v == null) {
			v = value(key);
			ref = ref(v);
			if (map.putIfAbsent(key, ref) != null) {
				while (true) {
					Reference<V> ref2 = map.get(key);
					V v2 = ref2.get();
					if (v2 != null) return v2;
					if (map.replace(key, ref2, ref)) {
						return v;
					}
				}
			}
		}
		return v;
	}
	
	protected Reference<V> ref(V value) {
		return new SoftReference<V>(value);
	}
	
	protected V value(K key) {
		return function.apply(key);
	}
	
	public void clear() {
		map.clear();
	}
	
	public Iterable<V> values() {
		return () -> new Iterator<V>() {
			Iterator<Reference<V>> it = map.values().iterator();
			V next = null;
			
			@Override
			public boolean hasNext() {
				if (next != null) return true;
				while (next == null && it.hasNext()) {
					next = it.next().get();
				}
				return next != null;
			}

			@Override
			public V next() {
				if (!hasNext()) throw new NoSuchElementException();
				V n = next;
				next = null;
				return n;
			}
		};
	}
	
}
