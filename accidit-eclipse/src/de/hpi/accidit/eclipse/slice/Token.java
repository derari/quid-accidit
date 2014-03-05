package de.hpi.accidit.eclipse.slice;

public class Token implements Comparable<Token> {
	
	public static Token variable(String var, int line) {
		return new Token(line + ":" + var);
	}
	
	public static Token result(int line) {
		return new Token(line + ":" + "<return>");
	}
	
//	public static Token element(int index, int line) {
//		return new Token("[" + index + "]:" + line);
//	}
//	
//	public static Token field(String name, int line) {
//		return new Token("[" + name + "]:" + line);
//	}
	
	private final String key;
	
	private Token(String key) {
		this.key = key;
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
