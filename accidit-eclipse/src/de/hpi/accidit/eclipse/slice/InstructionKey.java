package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class InstructionKey implements Comparable<InstructionKey> {
	
	public static InstructionKey variable(String var, CodeIndex ci) {
		return new InstructionKey(ci, var, var);
	}
	
	public static InstructionKey array(CodeIndex ci) {
		return new InstructionKey(ci, "<array>");
	}
	
	public static InstructionKey field(String field, CodeIndex ci) {
		return new InstructionKey(ci, "<set>" + field);
	}
	
	public static InstructionKey result(CodeIndex ci) {
		return new InstructionKey(ci, "<return>");
	}
	
	public static InstructionKey thrown(CodeIndex ci) {
		return new InstructionKey(ci, "<throw>");
	}
	
	public static InstructionKey invoke(String methodKey, CodeIndex ci) {
		return new InstructionKey(ci, "<invoke>" + methodKey);
	}
	
	public static InstructionKey invoke(String clazz, String method, String sig, CodeIndex ci) {
		return invoke(methodKey(clazz, method, sig), ci);
	}
	
	public static InstructionKey invokeThis(String methodKey, CodeIndex ci) {
		return new InstructionKey(ci, "<invoke>" + methodKey + "[this]");
	}
	
	public static InstructionKey invokeThis(String clazz, String method, String sig, CodeIndex ci) {
		return invokeThis(methodKey(clazz, method, sig), ci);
	}
	
	public static InstructionKey invokeArg(String methodKey, int argIndex, CodeIndex ci) {
		return new InstructionKey(ci, "<invoke>" + methodKey + "[" + argIndex + "]");
	}
	
	public static InstructionKey invokeArg(String clazz, String method, String sig, int argIndex, CodeIndex ci) {
		return invokeArg(methodKey(clazz, method, sig), argIndex, ci);
	}
	
	public static String methodKey(String clazz, String method, String sig) {
		return clazz + "#" + method + sig;
	}
	
//	public static Token element(int index, CodeIndex ci) {
//		return new Token("[" + index + "]:" + line);
//	}
//	
//	public static Token field(String name, CodeIndex ci) {
//		return new Token("[" + name + "]:" + line);
//	}
	
	private final CodeIndex ci;
	private final String key;
	private final String var;
	
	private InstructionKey(CodeIndex ci, String key) {
		this.ci = ci;
		this.key = key;
		this.var = null;
	}
	
	private InstructionKey(CodeIndex ci, String key, String var) {
		this.ci = ci;
		this.key = key;
		this.var = var;
	}
	
	public CodeIndex getCodeIndex() {
		return ci;
	}
	
	public String getVar() {
		return var;
	}
	
	public <E> List<E> getValuesFrom(Map<InstructionKey, E> map) {
		if (getCodeIndex().getLineIndex() >= 0) {
			E e = map.get(this);
			if (e == null) return Collections.emptyList();
			else return Arrays.asList(e);
		}
		List<E> result = new ArrayList<>();
		for (Map.Entry<InstructionKey, E> e: map.entrySet()) {
			if (equals(e.getKey())) result.add(e.getValue());
		}
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InstructionKey)) return false;
		InstructionKey t = (InstructionKey) obj;
		if (!ci.equals(t.ci)) return false;
		// don't compare class name in case of polymorphism
		String k1 = key.substring(key.indexOf('#')+1);
		String k2 = t.key.substring(t.key.indexOf('#')+1);
		return k1.equals(k2);
	}
	
	@Override
	public int hashCode() {
		return ci.hashCode() ^ key.hashCode();
	}
	
	@Override
	public String toString() {
		return ci + ":" + key;
	}

	@Override
	public int compareTo(InstructionKey o) {
		int c;
		c = ci.getLine() - o.ci.getLine();
		if (c != 0) return c;
		if (ci.getLineIndex() > -1 && o.ci.getLineIndex() > -1) {
			c = ci.getLineIndex() - o.ci.getLineIndex();
			if (c != 0) return c;
		}
		return key.compareTo(o.key);
	}
}
