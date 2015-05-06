package de.hpi.accidit.eclipse.slice;

public class Token implements Comparable<Token> {
	
	public static Token variable(String var, int line) {
		return new Token(line + ":" + var, var);
	}
	
	public static Token array(int line) {
		return new Token(line + ":" + "<array>");
	}
	
	public static Token field(String field, int line) {
		return new Token(line + ":" + "<set>" + field);
	}
	
	public static Token result(int line) {
		return new Token(line + ":" + "<return>");
	}
	
	public static Token thrown(int line) {
		return new Token(line + ":" + "<throw>");
	}
	
	public static Token invoke(String methodKey, int line) {
		return new Token(line + ":" + "<invoke>" + methodKey);
	}
	
	public static Token invoke(String clazz, String method, String sig, int line) {
		return invoke( methodKey(clazz, method, sig), line);
	}
	
	public static Token invokeThis(String methodKey, int line) {
		return new Token(line + ":" + "<invoke>" + methodKey + "[this]");
	}
	
	public static Token invokeThis(String clazz, String method, String sig, int line) {
		return invokeThis(methodKey(clazz, method, sig), line);
	}
	
	public static Token invokeArg(String methodKey, int argIndex, int line) {
		return new Token(line + ":" + "<invoke>" + methodKey + "[" + argIndex + "]");
	}
	
	public static Token invokeArg(String clazz, String method, String sig, int argIndex, int line) {
		return invokeArg( methodKey(clazz, method, sig), argIndex, line);
	}
	
	public static String methodKey(String clazz, String method, String sig) {
		return clazz + "#" + method + sig;
	}
	
//	public static Token element(int index, int line) {
//		return new Token("[" + index + "]:" + line);
//	}
//	
//	public static Token field(String name, int line) {
//		return new Token("[" + name + "]:" + line);
//	}
	
	private final String key;
	private final String var;
	private final int id;
	
	private Token(String key) {
		this.key = key;
		this.var = null;
		this.id = 0;
	}
	
	public Token(String key, String var) {
		this.key = key;
		this.var = var;
		this.id = 0;
	}
	
	private Token(String key, String var, int id) {
		this.key = key + "_" + id;
		this.var = var;
		this.id = id;
	}
	
	public Token makeUnique() {
		String key = id == 0 ? this.key : this.key.substring(0, this.key.lastIndexOf('_'));
		return new Token(key, var, id+1);
	}
	
	public String getVar() {
		return var;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Token)) return false;
		return key.equals(obj.toString());
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@Override
	public String toString() {
		return key;
	}

	@Override
	public int compareTo(Token o) {
		return key.compareTo(o.key);
	}
}
