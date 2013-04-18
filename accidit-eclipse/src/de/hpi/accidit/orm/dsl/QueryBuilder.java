package de.hpi.accidit.orm.dsl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OPreparedStatement;
import de.hpi.accidit.orm.dsl.QueryTemplate.Attribute;
import de.hpi.accidit.orm.dsl.QueryTemplate.Condition;
import de.hpi.accidit.orm.dsl.QueryTemplate.Join;
import de.hpi.accidit.orm.dsl.QueryTemplate.Relation;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.MultiValueAdapter;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;
import de.hpi.accidit.orm.map.IdMap;
import de.hpi.accidit.orm.map.ValueAdapterBase;

import java.util.Collections;

public class QueryBuilder<Result> {
	
	private final OConnection cnn;
	private final QueryTemplate template;
	private final Mapping<Result> mapping;
	
	private final List<String>  	resultAttributes = new ArrayList<>();
	private final Set<String>  		resultAttributesGuard = new HashSet<>();
	private final List<Attribute> 	selectedAttributes = new ArrayList<>();
	private final Set<String>  		selectedAttributeKeys = new HashSet<>();
	private final List<Join> 		joins = new ArrayList<>();
	private final Set<String>  		joinKeys = new HashSet<>();
	private final List<Condition> 	conditions = new ArrayList<>();
	private final List<Object[]> 	conditionArguments = new ArrayList<>();
	private final Map<String, Integer>	conditionIndizes = new HashMap<>();
	private int totalArgCount = 0;
	private final Map<String, SelectedRelation> relations = new HashMap<>();
	
	
	private final List<String>		publicResultAttributes = Collections.unmodifiableList(resultAttributes);

	public QueryBuilder(QueryTemplate template, Mapping<Result> mapping) {
		this(null, template, mapping);
	}

	public QueryBuilder(OConnection cnn, QueryTemplate template, Mapping<Result> mapping) {
		this.cnn = cnn;
		this.template = template;
		this.mapping = mapping;
	}

	private void _addDependencies(String[] required) {
		for (String key: required) {
			if (template.getAttribute(key) != null) {
				_selectAttribute(key, false);
			}
			if (template.getJoin(key) != null) {
				_joinTable(key);
			}
		}
	}

	private void _selectResultAttribute(String key) {
		int dot = key.indexOf('.');
		if (dot == 0) throw new IllegalArgumentException(key);
		String part2 = dot < 0 ? null : key.substring(dot+1);
		if (dot > 0) key = key.substring(0, dot);
		
		if (resultAttributesGuard.add(key)) {
			_selectAttribute(key, true);
		}
		
		if (part2 != null) {
			SelectedRelation sr = relations.get(key);
			sr.select(part2);
		}
		
	}
	
	private void _selectAttribute(String key, boolean result) {
		if (selectedAttributeKeys.add(key)) {
			Attribute a = template.getAttribute(key);
			if (a != null) {
				_addDependencies(a.getDependencies());
				selectedAttributes.add(a);
				if (result) resultAttributes.add(key);
			}
			Relation r = template.getRelation(key);
			if (r != null) {
				_addDependencies(r.getDependencies());
				relations.put(key, new SelectedRelation(r));
			}
		}
	}
	
	private void _joinTable(String key) {
		if (joinKeys.add(key)) {
			Join j = template.getJoin(key);
			_addDependencies(j.getDependencies());
			joins.add(j);
		}
	}
	
	private int _addCondition(String alias, String key) {
		int index = conditions.size();
		Integer oldIndex = conditionIndizes.put(alias, index);
		if (oldIndex != null)  conditionIndizes.put(alias, -1);
		
		Condition c = template.getCondition(key);
		__addCondition(c);
		
		return index;
	}
	
	private int _addCondition(Condition c) {
		int index = conditions.size();
		__addCondition(c);
		return index;
	}
	
	private void __addCondition(Condition c) {
		_addDependencies(c.getDependencies());
		conditions.add(c);
		
		int paramC = c.getParamCount();
		if (paramC > 0) {
			conditionArguments.add(new Object[paramC]);
			totalArgCount += paramC;
		} else {
			conditionArguments.add(null);
		}
	}
	
	private void _setArguments(int index, Object[] arguments) {
		Object[] argValues = conditionArguments.get(index);
		System.arraycopy(arguments, 0, argValues, 0, argValues.length);
	}
	
	protected static class SelectedRelation {
		private final Relation r;
		private List<String> selected = null;
		public SelectedRelation(Relation r) {
			this.r = r;
		}
		protected void select(String attribute) {
			if (selected == null) selected = new ArrayList<>();
			selected.add(attribute);
		}
		public Relation getRelation() {
			return r;
		}
		public String[] getAttributes() {
			if (selected == null) return null;
			return selected.toArray(new String[selected.size()]);
		}
	}
	
	protected static class RelationAdapter<Entity> extends ValueAdapterBase<Entity> {

		private final OConnection cnn;
		private final SelectedRelation sr;
		private final Mapping<Entity> mapping;
		private final List<Entity> records = new ArrayList<>();
		private final List<Object> keys = new ArrayList<>();
		private final IdMap<SQLException> idMap;
		private ResultSet rs = null;
		private int[] keyIndices = null;

		public RelationAdapter(OConnection cnn, SelectedRelation sr, Mapping<Entity> mapping) {
			this.cnn = cnn;
			this.sr = sr;
			this.mapping = mapping;
			idMap = new IdMap<SQLException>(sr.r.getReferenceKeys().length) {
				@Override
				protected Object[] fetchValues(Object[] keys) throws SQLException {
					return RelationAdapter.this.fetchValues(keys);
				}
			};
		}
		
		@Override
		public void initialize(ResultSet rs) throws SQLException {
			this.rs = rs;
			this.keyIndices = getFieldIndices(rs, sr.r.getReferenceKeys());
		}
		
		@Override
		public void apply(Entity record) throws SQLException {
			final Object key;
			if (keyIndices.length == 1)  {
				key = rs.getObject(keyIndices[0]);
			} else {
				final Object[] keyArray = new Object[keyIndices.length];
				for (int i = 0; i < keyArray.length; i++) {
					keyArray[i] = rs.getObject(keyIndices[i]);
				}
				key = keyArray;
			}
			records.add(record);
			keys.add(key);
		}

		@Override
		public void complete() throws SQLException {
			final Object[] keyArray = keys.toArray();
			final Object[] values = idMap.getAll(keyArray);
			String field = sr.r.getAttribute();
			for (int i = 0; i < values.length; i++) {
				Entity r = records.get(i);
				mapping.setField(r, field, values[i]);
			}
		}

		protected Object[] fetchValues(Object[] keys) throws SQLException {
			return cnn
					.select(sr.getAttributes())
					.from(sr.r.getView())
					.byKeys(keys)
					.run().asArray();
		}
		
	}
	
	protected void select(String... attributes) {
		if (attributes != null) {
			for (String key: attributes) {
				if (key.equals("*")) {
					select((String[]) null);
				} else {
					_selectResultAttribute(key);
				}
			}
		} else {
			for (String key: template.getSelectableAttributes()) {
				_selectResultAttribute(key);
			}
		}
	}
	
	protected void where(String key, Object... arguments) {
		addCondition(key, arguments);
	}
	
	protected void where(Condition c, Object... arguments) {
		addCondition(c, arguments);
	}
	
	protected void addCondition(String key, Object... arguments) {
		addCondition(key, key, arguments);
	}
	
	/**
	 * Assigning an alias to a condition allows to change the arguments 
	 * if the same condition is used multiple times.
	 * 
	 * Disabled for now.
	 */
	private void addCondition(String alias, String key, Object... arguments) {
		int index = _addCondition(alias, key);
		if (arguments != null && arguments.length > 0) {
			_setArguments(index, arguments);
		}
	}
	
	protected void addCondition(Condition c, Object... arguments) {
		int index = _addCondition(c);
		if (arguments != null && arguments.length > 0) {
			_setArguments(index, arguments);
		}
	}
	
	protected Condition c(String condition) {
		return new Condition(condition);
	}
	
//	public void setCondition(String key, Object... arguments) {
//		setCondition(key, key, arguments);
//	}
//	
//	public void setCondition(String alias, String key, Object... arguments) {
//		Integer index = conditionIndizes.get(alias);
//		if (index == null) index = _addCondition(alias, key);
//		if (index < 0) throw new IllegalArgumentException("Condition alias not unique: " + alias);
//		if (arguments != null && arguments.length > 0) {
//			_setArguments(index, arguments);
//		}
//	}
	
	protected String queryString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT ");
		boolean firstSelect = true;
		for (Attribute a: selectedAttributes) {
			if (firstSelect) firstSelect = false;
			else sb.append(", ");
			sb.append(a.getDefinition());
		}
		
		sb.append("\nFROM ");
		sb.append(template.getMainTable());
		for (Join j: joins) {
			sb.append("\n");
			j.appendClause(sb);
		}
		
		boolean firstCondition = true;
		for (Condition c: conditions) {
			if (firstCondition) {
				firstCondition = false;
				sb.append("\nWHERE ");
			} else {
				sb.append("\n  AND ");
			}
			sb.append(c.getCondition());
		}
		
		sb.append(';');
		return sb.toString();
	}
	
	protected Object[] arguments() {
		return flatten(conditionArguments, totalArgCount);
	}
	
	protected ValueAdapter<Result> valueAdapter(OConnection cnn) {
		int size = relations.size();
		if (size == 0) {
			return mapping.getValueAdapter(publicResultAttributes);
		} else {
			@SuppressWarnings("unchecked")
			ValueAdapter<Result>[] pp = new ValueAdapter[size+1];
			pp[0] = mapping.getValueAdapter(publicResultAttributes);
			int i = 1;
			for (SelectedRelation sr: relations.values()) {
				pp[i++] = new RelationAdapter<Result>(cnn, sr, mapping);
			}
			return new MultiValueAdapter<>(pp);
		}
	}
	
	protected Object[] flatten(Iterable<Object[]> arrays) {
		return flatten(arrays, getTotalLen(arrays));
	}
	
	protected Object[] flatten(Object[][] arrays) {
		return flatten(arrays, getTotalLen(arrays));
	}
	
	private int getTotalLen(Iterable<Object[]> arrays) {
		int total = 0;
		for (Object[] a: arrays) {
			if (a != null) total += a.length;
		}
		return total;
	}
	
	private int getTotalLen(Object[][] arrays) {
		int total = 0;
		for (Object[] a: arrays) {
			if (a != null) total += a.length;
		}
		return total;
	}
	
	protected Object[] flatten(Iterable<Object[]> arrays, int totalLen) {
		final Object[] result = new Object[totalLen];
		int i = 0;
		for (Object[] a: arrays) {
			if (a != null) {
				int len = a.length;
				System.arraycopy(a, 0, result, i, len);
				i += len;
			}
		}
		assert i == result.length;
		return result;
	}
	
	protected Object[] flatten(Object[][] arrays, int totalLen) {
		final Object[] result = new Object[totalLen];
		int i = 0;
		for (Object[] cArgs: arrays) {
			if (cArgs != null) {
				int len = cArgs.length;
				System.arraycopy(cArgs, 0, result, i, len);
				i += len;
			}
		}
		assert i == result.length;
		return result;
	}
	
	public Submit<Result> submit(OConnection cnn) throws SQLException {
		OPreparedStatement ps = cnn.prepare(queryString());
		OFuture<ResultSet> result = ps.submit(arguments());
		ValueAdapter<Result> va = valueAdapter(cnn);
		return new Submit<>(result, mapping, va);
	}
	
	public Submit<Result> submit() throws SQLException {
		return submit(cnn);
	}
	
	public Run<Result> run(OConnection cnn) throws SQLException {
		OPreparedStatement ps = cnn.prepare(queryString());
		ResultSet result = ps.run(arguments());
		ValueAdapter<Result> va = valueAdapter(cnn);
		return new Run<>(result, mapping, va);
	}
	
	public Run<Result> run() throws SQLException {
		return run(cnn);
	}
	
}
