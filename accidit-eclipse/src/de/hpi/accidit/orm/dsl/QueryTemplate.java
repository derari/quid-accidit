package de.hpi.accidit.orm.dsl;

import java.util.HashMap;
import java.util.Map;

public class QueryTemplate {
	
	private final Map<String, String> attributes = new HashMap<>();
	private String mainTable = null;
	private final Map<String, Join> autoJoins = new HashMap<>();
	private final Map<String, Join> conditions = new HashMap<>();
	
	
	protected void attribute(String key, String definition) {
		attributes.put(key, definition);
	}
	
	protected void attributes(String... definitions) {
		for (int i = 0; i < definitions.length; i += 2) {
			attribute(definitions[i], definitions[i]);
		}
	}
	
	protected void table(String table) {
		this.mainTable = table;
	}
	
	protected Join autoJoin(String key, String table) {
		Join j = new Join(table, null);
		autoJoins.put(key, j);
		return j;
	}
	
	protected void condition(String key, String condition) {
		int paramC = countParameters(condition);
		Condition c = new Condition(condition, paramC);
	}

	private int countParameters(String s) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '?') count++;
		}
		return count;
	}
	
	public static class Join {
		private final String table;
		private String condition;
		public Join(String table, String on) {
			super();
			this.table = table;
			this.condition = on;
		}
		public void on(String condition) {
			this.condition = condition;
		}
	}

	public static class Condition {
		private final String condition;
		private final int paramC;
		public Condition(String condition, int paramC) {
			super();
			this.condition = condition;
			this.paramC = paramC;
		}
	}
	
	
	
}
