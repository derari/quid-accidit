package de.hpi.accidit.eclipse.slice;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class MethodDataDependencyCache {
	
//	private static final MethodDataDependencyCache cache = new MethodDataDependencyCache();
//	
//	public static Map<Token, DataDependency> getDependencyGraph(String clazz, String method, String signature) {
//		return cache._getDependencyGraph(clazz, method, signature);
//	}
//
//	private final Map<String, WeakReference<Map<Token, DataDependency>>> map = new HashMap<>();
//	
//	protected MethodDataDependencyCache() {
//	}
//	
//	public synchronized Map<Token, DataDependency> _getDependencyGraph(String clazz, String method, String signature) {
//		String key = clazz + "#" + method + signature;
//		WeakReference<Map<Token, DataDependency>> ref = map.get(key);
//		Map<Token, DataDependency> graph = ref != null ? ref.get() : null;
//		if (graph == null) {
//			graph = MethodDataDependencyAnalysis.analyseMethod(clazz, method, signature);
//			ref = new WeakReference<>(graph);
//			map.put(key, ref);
//		}
//		return graph;
//	}
}
