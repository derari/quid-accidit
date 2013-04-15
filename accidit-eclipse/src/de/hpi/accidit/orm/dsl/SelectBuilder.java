package de.hpi.accidit.orm.dsl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class SelectBuilder {

	private final Map<String, String> fields = new HashMap<>();
	private final List<String> fieldKeys = new ArrayList<>();
	private final List<String> fieldKeysPublic = Collections.unmodifiableList(fieldKeys);
	
	protected void addFields(String... fields) {
		for (int i = 0; i < fields.length; i += 2) {
			String key = fields[i];
			String old = this.fields.put(key, fields[i+1]);
			if (old != null) {
				throw new IllegalArgumentException(
						"Duplicate field: " + key + " -> " + old + " / -> " + fields[i+1]);
			}
			fieldKeys.add(key);
		}
	}
	
	public boolean appendFields(StringBuilder sb, String[] select) {
		boolean added = false;
		if (select == null) {
			for (String key: fieldKeys) {
				if (added) sb.append(", ");
				else added = true;
				sb.append(fields.get(key));
			}
		} else {
			for (String key: select) {
				if (added) sb.append(", ");
				else added = true;
				String f = fields.get(key);
				if (f == null) {
					throw new IllegalArgumentException("Invalid field :" + key);
				}
				sb.append(f);
			}
		}
		return added;
	}
	
	public List<String> fieldKeys(String[] select) {
		if (select == null) {
			return fieldKeysPublic;
		} else {
			return Arrays.asList(select.clone());
		}
	}
	
}
