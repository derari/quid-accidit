package de.hpi.accidit.orm.dsl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OPreparedStatement;
import de.hpi.accidit.orm.cursor.ResultCursor;
import de.hpi.accidit.orm.dsl.QueryTemplate.AdapterTemplate;
import de.hpi.accidit.orm.dsl.QueryTemplate.Attribute;
import de.hpi.accidit.orm.dsl.QueryTemplate.Condition;
import de.hpi.accidit.orm.dsl.QueryTemplate.Join;
import de.hpi.accidit.orm.dsl.QueryTemplate.OrderBy;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.MultiValueAdapter;
import de.hpi.accidit.orm.map.ResultBuilder;
import de.hpi.accidit.orm.map.ValueAdapterBase;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapterFactory;

public class QueryBuilder<Result> {
	
	private final OConnection cnn;
	private final QueryTemplate<Result> template;
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
	private final Map<String, SelectedAdapter<Result>> adapters = new HashMap<>();
	private final List<Ordering> 	orderings = new ArrayList<>();
	private final Set<String>  		orderingKeys = new HashSet<>();
	
	private final List<String>		publicResultAttributes = Collections.unmodifiableList(resultAttributes);

	public QueryBuilder(QueryTemplate<Result> template, Mapping<Result> mapping) {
		this(null, template, mapping);
	}

	public QueryBuilder(OConnection cnn, QueryTemplate<Result> template, Mapping<Result> mapping) {
		this.cnn = cnn;
		this.template = template;
		this.mapping = mapping;
	}

	private void _addDependencies(String[] required) {
		for (String key: required) {
			if (template.getAttribute(key) != null) {
				_selectAttribute(key);
			}
			if (template.getJoin(key) != null) {
				_joinTable(key);
			}
			if (template.getOrderBy(key) != null) {
				_ordering(key, null);
			}
		}
	}

	private void _selectResultAttribute(String key) {
		int dot = key.indexOf('.');
		if (dot == 0) throw new IllegalArgumentException(key);
		String part2 = dot < 0 ? null : key.substring(dot+1);
		if (dot > 0) key = key.substring(0, dot);
		
		if (resultAttributesGuard.add(key)) {
			_selectAttribute(key);
			if (template.getAttribute(key) != null) {
				resultAttributes.add(key);
			}
		}
		
		if (part2 != null) {
			SelectedAdapter<Result> sr = adapters.get(key);
			sr.select(part2);
		}
		
	}
	
	private void _selectAttribute(String key) {
		if (selectedAttributeKeys.add(key)) {
			Attribute a = template.getAttribute(key);
			if (a != null) {
				_addDependencies(a.getDependencies());
				selectedAttributes.add(a);
			}
			AdapterTemplate<Result> at = template.getAdapter(key);
			if (at != null) {
				_addDependencies(at.getDependencies());
				_addValueAdapter(key, at);
			}
		}
	}
	
	private void _addValueAdapter(String key, ValueAdapterFactory<Result> vaf) {
		adapters.put(key, new SelectedAdapter<Result>(vaf));
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
	
	private void _ordering(String key, Boolean asc) {
		if (orderingKeys.add(key)) {
			OrderBy ob = template.getOrderBy(key);
			_addDependencies(ob.getDependencies());
			Ordering o = new Ordering(ob, asc);
			orderings.add(o);
		}
	}
	
	protected static class SelectedAdapter<E> {
		private final ValueAdapterFactory<E> r;
		private List<String> selected = null;
		public SelectedAdapter(ValueAdapterFactory<E> r) {
			this.r = r;
		}
		protected void select(String attribute) {
			if (selected == null) selected = new ArrayList<>();
			selected.add(attribute);
		}
		public ValueAdapterFactory<E> getAdapterTemplate() {
			return r;
		}
		public List<String> getSelectedAttributes() {
			return selected;
		}
	}
	
	protected static class Ordering {
		private final OrderBy ob;
		private final Boolean asc;
		public Ordering(OrderBy ob, Boolean asc) {
			this.ob = ob;
			this.asc = asc;
		}
		public String getField() {
			return ob.getField();
		}
		public boolean isAsc() {
			return asc != null ? asc : ob.defaultAsc();
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
	
	protected void orderBy(String key, Boolean asc) {
		_ordering(key, asc);
	}
	
	protected void orderBy(String key) {
		_ordering(key, null);
	}
	
	protected void orderByAsc(String key) {
		orderBy(key, true);
	}
	
	protected void orderByDesc(String key) {
		orderBy(key, false);
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
	
	private int i = 0;
	
	protected void apply(ValueAdapterFactory<Result> vaf) {
		String key = "$vaf$" + i;
		_addValueAdapter(key, vaf);
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
		
		boolean firstOrderBy = true;
		for (Ordering o: orderings) {
			if (firstOrderBy) {
				firstOrderBy = false;
				sb.append("\nORDER BY ");
			} else {
				sb.append(", ");
			}
			sb.append(o.getField());
			sb.append(o.isAsc() ? " ASC" : " DESC");
		}
		
		sb.append(';');
		return sb.toString();
	}
	
	protected Object[] arguments() {
		return flatten(conditionArguments, totalArgCount);
	}
	
	protected ValueAdapter<Result> buildValueAdapter(OConnection cnn) {
		int size = adapters.size();
		if (size == 0) {
			return mapping.getValueAdapter(publicResultAttributes);
		} else {
			@SuppressWarnings("unchecked")
			ValueAdapter<Result>[] pp = new ValueAdapter[size+1];
			pp[0] = mapping.getValueAdapter(publicResultAttributes);
			int i = 1;
			for (SelectedAdapter<Result> sr: adapters.values()) {
				pp[i++] = sr.getAdapterTemplate().newAdapter(mapping, cnn, sr.getSelectedAttributes());
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
	
	public ResultSet run() throws SQLException {
		return run(cnn);
	}
	
	public ResultSet run(OConnection cnn) throws SQLException {
		OPreparedStatement ps = cnn.prepare(queryString());
		return ps.run(arguments());
	}
	
	public OFuture<ResultSet> submit() throws SQLException {
		return submit(cnn);
	}
	
	public OFuture<ResultSet> submit(OConnection cnn) throws SQLException {
		OPreparedStatement ps = cnn.prepare(queryString());
		return ps.submit(arguments());
	}
	
	public <R> Submittable<R> as(ResultBuilder<R, Result> rb) {
		return new Submittable<>(cnn, this, rb, mapping.getEntityFactory());
	}
	
	public Submittable<Result[]> asArray() {
		return as(mapping.asArray());
	}
	
	public Submittable<List<Result>> asList() {
		return as(mapping.asList());
	}
	
	public Submittable<ResultCursor<Result>> asCursor() {
		return as(mapping.asCursor());
	}
	
	public Submittable<Result> getSingle() {
		return as(mapping.getSingle());
	}
	
	public Submittable<Result> getFirst() {
		return as(mapping.getFirst());
	}
	
}
