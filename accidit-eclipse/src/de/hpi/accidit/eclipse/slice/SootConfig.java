package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import soot.G;
import soot.Scene;
import soot.options.Options;

public class SootConfig {
	
	private final IJavaProject p;
	
	public SootConfig(IJavaProject p) {
		this.p = p;
	}
	
	private List<String> getClassPath() {
		try {
			List<String> result = new ArrayList<>();
			for (IClasspathEntry ce: p.getResolvedClasspath(true)) {
				result.add(ce.getPath().toOSString());
			}
			return result;
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Map<Token, DataDependency> analyse(String clazz, String method, String signature) {
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
	
	private static synchronized boolean enable(SootConfig cfg) {
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
				sb.append(s);
				sb.append(sep);
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
		l.lock();
		return l;
	}
}
