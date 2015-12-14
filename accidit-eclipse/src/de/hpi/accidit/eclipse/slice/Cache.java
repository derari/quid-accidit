package de.hpi.accidit.eclipse.slice;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class for lazily caching objects.
 * @param <K>
 * @param <V>
 */
public abstract class Cache<K, V> {
	
	private final ConcurrentMap<K, Reference<V>> map = new ConcurrentHashMap<>();

	public Cache() {
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
	
	protected abstract V value(K key);
	
}
