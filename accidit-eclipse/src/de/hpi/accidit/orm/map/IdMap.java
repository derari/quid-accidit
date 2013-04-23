package de.hpi.accidit.orm.map;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class IdMap<E extends Exception> {
	
	private final Object NULL = new Object();
	
	private final Map<Object, Object> map = new HashMap<>();
	private final int keyWidth;
	
	public IdMap(int keyWidth) {
		this.keyWidth = keyWidth;
	}

	public Object[] getAll(final Object[] keys) throws E {
		
		final Object[] result = new Object[keys.length];
		
		int missingCount = fill(result, keys);
		
		if (missingCount > 0) {
			fillMissing(result, keys, missingCount);
		}
		
		return result;
	}

	private int fill(final Object[] values, final Object[] keys) {
		int missingCount = 0;
		MultiKey mk = keyWidth > 1 ? new MultiKey() : null;
		for (int i = 0; i < keys.length; i++) {
			if (values[i] == null) {
				final Object o;
				if (mk == null) {
					o = get(keys[i]);
				} else {
					mk.become(keys[i]);
					o = get(mk);
				}
				if (o == null) {
					missingCount++;
				} else if (o != NULL) {
					values[i] = o;
				}
			}
		}
		return missingCount;
	}
	
	private void fillMissing(final Object[] result, final Object[] keys, int missingCount) throws E {
		Set<Object> missingKeySet = new HashSet<>();
		for (int i = 0; i < result.length; i++) {
			if (result[i] == null) {
				Object key = keys[i];
				if (keyWidth > 1) key = new MultiKey((Object[]) key);
				missingKeySet.add(key);
			}
		}
		
		final Object[] missingKeys = missingKeySet.toArray(new Object[missingKeySet.size()]);
		final Object[] missingKeysRaw = keyWidth > 1 ? rawKeys(missingKeys) : missingKeys;
		final Object[] missingValues = fetchValues(missingKeysRaw);
		
		for (int i = 0; i < missingKeys.length; i++) {
			Object o = missingValues[i];
			if (o == null) o = NULL;
			map.put(missingKeys[i], o);
		}
		
		missingCount = fill(result, keys);
		assert missingCount == 0;
	}

	private Object[] rawKeys(final Object[] keys) {
		final Object[] result = new Object[keys.length];
		for (int i = 0; i < keys.length; i++) {
			result[i] = ((MultiKey) keys[i]).keys;
		}
		return result;
	}

	protected abstract Object[] fetchValues(Object[] keys) throws E;

	protected Object get(Object key) {
		return map.get(key);
	}

	protected static class MultiKey {
		
		private Object[] keys;
		private int hash = -1;
		
		public MultiKey(Object[] ids) {
			this.keys = ids.clone();
		}
		
		public MultiKey() {
			keys = null;
		}
		
		private void become(Object key) {
			this.keys = (Object[]) key;
			hash = -1;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof MultiKey)) return false;
			final MultiKey o = (MultiKey) obj;
			return Arrays.equals(keys, o.keys);
		}
		
		@Override
		public int hashCode() {
			if (hash == -1) {
				hash = Arrays.hashCode(keys);
				if (hash == -1) hash = 0;
			}
			return hash;
		}
		
	}

}
