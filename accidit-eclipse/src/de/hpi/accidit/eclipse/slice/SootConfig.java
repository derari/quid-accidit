package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import soot.G;
import soot.Scene;
import soot.options.Options;

public abstract class SootConfig {
	
	protected abstract List<String> getClassPath();
	
	private final Cache<String, Map<Token, DataDependency>> graphCache = new Cache<String, Map<Token,DataDependency>>() {
		@Override
		protected Map<Token, DataDependency> value(String key) {
			return doAnalyse(key);
		}
	};
	
	public Map<Token, DataDependency> analyse(String methodId) {
		return graphCache.get(methodId);
	}
	
	private Map<Token, DataDependency> doAnalyse(String methodId) {
		int c = methodId.indexOf('#');
		int s = methodId.indexOf('(');
		return doAnalyse(methodId.substring(0, c), methodId.substring(c+1, s), methodId.substring(s));
	}
	
	public Map<Token, DataDependency> doAnalyse(String clazz, String method, String signature) {
		Lock l = lockSootFor(this);
		if (l == null) return null;
		try {
			return MethodDataDependencyAnalysis.analyseMethod(clazz, method, signature);
		} finally {
			l.unlock();
		}
	}
	
	private static SootConfig current = null;
	private static final ReadWriteLock analysisLock = new ReentrantReadWriteLock();
	
	static synchronized boolean enable(SootConfig cfg) {
		if (current == cfg) return true;
		Lock l = analysisLock.writeLock();
		try {
			l.lockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		try {
			G.reset();
			Options.v().parse(new String[]{"-keep-line-number", "-p", "jb", "use-original-names:true"});
			String cp = Scene.v().defaultClassPath();
			String sep = System.getProperty("path.separator");
			StringBuilder sb = new StringBuilder(cp);
			for (String s: cfg.getClassPath()) {
				sb.append(sep);
				sb.append(s);
			}
			Scene.v().setSootClassPath(sb.toString());
			current = cfg;
		} finally {
			l.unlock();
		}
		return true;
	}

	private static synchronized Lock lockSootFor(SootConfig cfg) {
		if (!enable(cfg)) return null;
		Lock l = analysisLock.readLock();
		try {
			l.lockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}	
		return l;
	}
	
	public static class JavaProjectConfig extends SootConfig {
		private final IJavaProject p;
		
		public JavaProjectConfig(IJavaProject p) {
			this.p = p;
		}
		
		protected List<String> getClassPath() {
			try {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				List<String> result = new ArrayList<>();
				for (IClasspathEntry ce: p.getResolvedClasspath(true)) {
					resolveClassPathEntry(ce, root, result);
				}
				return result;
			} catch (JavaModelException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static class WorkspaceConfig extends SootConfig {
		@Override
		protected List<String> getClassPath() {
			System.out.println(":::::::::::::::::::::::::::::::::");
			List<String> result = new ArrayList<>();
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			for (IProject p: ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
				IJavaProject jp = JavaCore.create(p);
				try {
					for (IClasspathEntry ce: jp.getResolvedClasspath(true)) {
						resolveClassPathEntry(ce, root, result);
					}
				} catch (JavaModelException e) {
					e.printStackTrace(System.err);
				}
			}
			return new ArrayList<>(new LinkedHashSet<>(result));
		}
	}
	
	public static void resolveClassPathEntry(IClasspathEntry ce, IWorkspaceRoot root, List<String> result) {
		IPath path = ce.getOutputLocation();
		if (path == null) path = ce.getPath();
		
		IResource res = root.findMember(path);
		String newPath;
	    if (res != null) {
	    	newPath = res.getLocation().toOSString();
	    } else {
	    	newPath = path.toOSString();
	    }
	    int sizeBefore = result.size();
	    if (newPath != null) {
	    	result.add(newPath);
	    	int i = newPath.indexOf("/src/");
	    	if (i < 0) i = newPath.indexOf("\\src\\");
	    	if (i > 0) {
	    		result.add(newPath.substring(0, i) + "/bin");
	    		result.add(newPath.substring(0, i) + "/build/classes");
	    	}
	    }
	    for (int i = sizeBefore; i < result.size(); i++) {
	    	System.out.println(": " + result.get(i));
	    }
	}
}
