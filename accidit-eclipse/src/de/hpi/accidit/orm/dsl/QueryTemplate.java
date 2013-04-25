package de.hpi.accidit.orm.dsl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.map.IdMap;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapterFactory;
import de.hpi.accidit.orm.map.ValueAdapterBase;

public class QueryTemplate<E> {
	
	private String mainTable = null;
	private final List<String> selectableAttributes = new ArrayList<>();
	private final List<String> selectableAttributesPublic = Collections.unmodifiableList(selectableAttributes);
	private final Map<String, Attribute> attributes = new HashMap<>();
	private final Map<String, Join> autoJoins = new HashMap<>();
	private final Map<String, Condition> conditions = new HashMap<>();
	private final Map<String, OrderBy> orderBys = new HashMap<>();
	private final Map<String, AdapterTemplate<E>> adapters = new HashMap<>();
	
	public QueryTemplate() {
	}
	
	protected void from(String table) {
		this.mainTable = table;
	}
	
	protected void select(String key, String definition) {
		Attribute a = new Attribute(definition, NO_DEPENDENCIES);
		attributes.put(key, a);
		selectableAttributes.add(key);
	}
	
	protected void select(String... definitions) {
		for (int i = 0; i < definitions.length; i += 2) {
			select(definitions[i], definitions[i+1]);
		}
	}
	
	protected void where(String key, String condition) {
		int paramC = countParameters(condition);
		Condition c = new Condition(condition, paramC, NO_DEPENDENCIES);
		conditions.put(key, c);
	}
	
	protected void where(String... conditions) {
		for (int i = 0; i < conditions.length; i += 2) {
			where(conditions[i], conditions[i+1]);
		}
	}

	protected Join optinal_join(String key, String table) {
		Join j = new Join(table, null, NO_DEPENDENCIES);
		autoJoins.put(key, j);
		return j;
	}
	
	protected Join optional_join(String key, String table, String on) {
		Join j = new Join(table, on, NO_DEPENDENCIES);
		autoJoins.put(key, j);
		return j;
	}
	
	protected void optional_join(String key, String table, String on, String... required) {
		Join j = new Join(table, on, required);
		autoJoins.put(key, j);
	}
	
//
//  For foreign stuff, use `using()` instead
//
//	private Attribute foreignAttribute(String key, String definition) {
//		Attribute a = new Attribute(definition, null);
//		attributes.put(key, a);
//		attributeKeys.add(key);
//		return a;
//	}
	
	private void foreignAttribute(String key, String definition, String... required) {
		Attribute a = new Attribute(definition, required);
		attributes.put(key, a);
		selectableAttributes.add(key);
	}
	
//	private Condition foreignCondition(String key, String condition) {
//		int paramC = countParameters(condition);
//		Condition c = new Condition(condition, paramC, null);
//		conditions.put(key, c);
//		return c;
//	}
	
	private void foreignCondition(String key, String condition, String... required) {
		int paramC = countParameters(condition);
		Condition c = new Condition(condition, paramC, required);
		conditions.put(key, c);
	}

	protected void internal_select(String key, String definition) {
		Attribute a = new Attribute(definition, NO_DEPENDENCIES);
		attributes.put(key, a);
	}
	
	protected void internal_select(String... definitions) {
		for (int i = 0; i < definitions.length; i += 2) {
			internal_select(definitions[i], definitions[i+1]);
		}
	}
	
//	private Attribute internForeignAttribute(String key, String definition) {
//		Attribute a = new Attribute(definition, null);
//		attributes.put(key, a);
//		return a;
//	}
	
	private void internForeignAttribute(String key, String definition, String... required) {
		Attribute a = new Attribute(definition, required);
		attributes.put(key, a);
	}
	
	protected void orderBy(String key, String field, boolean defaultAsc) {
		OrderBy ob = new OrderBy(field, defaultAsc, NO_DEPENDENCIES);
		orderBys.put(key, ob);
	}
	
	protected void orderBy(String key, String field) {
		orderBy(key, field, true);
	}
	
	protected void orderByAsc(String key, String field) {
		orderBy(key, field, true);
	}
	
	protected void orderByDesc(String key, String field) {
		orderBy(key, field, false);
	}
	
	private void orderBy(String key, String field, boolean defaultAsc, String... required) {
		OrderBy ob = new OrderBy(field, defaultAsc, required);
		orderBys.put(key, ob);
	}
	
	protected void relation(String key, View<? extends QueryByKey<?>> view, String... required) {
		RelationAdapterFactory<E> raf = new RelationAdapterFactory<>(key, view, required);
		valueAdapter(key, raf, required);
	}
	
	protected void valueAdapter(String key, ValueAdapterFactory<E> vaf, String... required) {
		AdapterTemplate<E> at = new AdapterTemplate<>(vaf, required);
		adapters.put(key, at);
	}

	protected Using<E> using(String... dependencies) {
		return new Using<E>(this, dependencies);
	}
	
	private static int countParameters(String s) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '?') count++;
		}
		return count;
	}
	
	/* QueryBuilder */ String getMainTable() {
		return mainTable;
	}

	/* QueryBuilder */ Attribute getAttribute(String key) {
		return attributes.get(key);
	}
	
	/* QueryBuilder */ Join getJoin(String key) {
		return autoJoins.get(key);
	}
	
	/* QueryBuilder */ Condition getCondition(String key) {
		return conditions.get(key);
	}
	
	/* QueryBuilder */ AdapterTemplate<E> getAdapter(String key) {
		return adapters.get(key);
	}
	
	/* QueryBuilder */ OrderBy getOrderBy(String key) {
		return orderBys.get(key);
	}
	
	/* QueryBuilder */ List<String> getSelectableAttributes() {
		return selectableAttributesPublic;
	}
	
	private static final String[] NO_DEPENDENCIES = {};
	
	protected static class QueryPart {
		private String[] required;
		public QueryPart(String[] required) {
			this.required = required;
		}
		public void requires(String... required) {
			this.required = required;
		}
		/* QueryBuilder */ String[] getDependencies() {
			return required;
		}
	}
	
	protected static class Attribute extends QueryPart {
		private final String definition;
		public Attribute(String definition, String[] required) {
			super(required);
			this.definition = definition;
		}
		public String getDefinition() {
			return definition;
		}
	}
	
	protected static class Condition extends QueryPart {
		private final String condition;
		private final int paramC;
		public Condition(String condition, int paramC, String[] required) {
			super(required);
			this.condition = condition;
			this.paramC = paramC;
		}
		/* QueryBuilder */ Condition(String condition) {
			this(condition, countParameters(condition), NO_DEPENDENCIES);
		}
		/* QueryBuilder */ int getParamCount() {
			return paramC;
		}
		/* QueryBuilder */ String getCondition() {
			return condition;
		}
	}
	
	protected static class Join extends QueryPart {
		private final String table;
		private String condition;
		public Join(String table, String on, String[] required) {
			super(required);
			this.table = table;
			this.condition = on;
		}
		public Join on(String condition) {
			this.condition = condition;
			return this;
		}
		public void appendClause(StringBuilder sb) {
			if (condition == null){
				throw new IllegalStateException(
						"Condition not configured for JOIN " + table);
			}
			sb.append("JOIN ").append(table)
			  .append(" ON " ).append(condition);
		}
	}
	
	protected static class OrderBy extends QueryPart {
		private final String field;
		private final boolean defaultAsc;
		public OrderBy(String field, boolean defaultAsc, String[] required) {
			super(required);
			this.field = field;
			this.defaultAsc = defaultAsc;
		}
		public String getField() {
			return field;
		}
		public boolean defaultAsc() {
			return defaultAsc;
		}
	}
	
//	protected static class Relation extends QueryPart {
//		private final String attribute;
//		private final View<? extends QueryByKey<?>> view;
//		public Relation(String attribute, View<? extends QueryByKey<?>> view, String[] required) {
//			super(required);
//			this.attribute = attribute;
//			this.view = view;
//		}
//		public String getAttribute() {
//			return attribute;
//		}
//		public View<? extends QueryByKey<?>> getView() {
//			return view;
//		}
//		public String[] getReferenceKeys() {
//			return getDependencies();
//		}
//	}
	
	protected static class AdapterTemplate<E> extends QueryPart implements ValueAdapterFactory<E> {
		private final ValueAdapterFactory<E> f;
		public AdapterTemplate(ValueAdapterFactory<E> f, String[] required) {
			super(required);
			this.f = f;
		}
		@Override
		public ValueAdapter<E> newAdapter(Mapping<E> mapping, OConnection cnn, List<String> attributes) {
			return f.newAdapter(mapping, cnn, attributes);
		}
	}
	
	protected static class Using<E> {
		private final QueryTemplate<E> qt;
		private final String[] required;
		public Using(QueryTemplate<E> qt, String[] required) {
			this.qt = qt;
			this.required = required;
		}
		public Using<E> select(String key, String definition) {
			qt.foreignAttribute(key, definition, required);
			return this;
		}
		public Using<E> select(String... definitions) {
			for (int i = 0; i < definitions.length; i += 2) {
				select(definitions[i], definitions[i+1]);
			}
			return this;
		}
		public Using<E> where(String key, String condition) {
			qt.foreignCondition(key, condition, required);
			return this;
		}
		public Using<E> where(String... conditions) {
			for (int i = 0; i < conditions.length; i += 2) {
				where(conditions[i], conditions[i+1]);
			}
			return this;
		}
		public Using<E> optional_join(String key, String table, String on) {
			qt.optional_join(key, table, on, required);
			return this;
		}
		public Using<E> internal_select(String key, String definition) {
			qt.internForeignAttribute(key, definition, required);
			return this;
		}
		public Using<E> internal_select(String... definitions) {
			for (int i = 0; i < definitions.length; i += 2) {
				internal_select(definitions[i], definitions[i+1]);
			}
			return this;
		}
		public Using<E> orderBy(String key, String field, boolean defaultAsc) {
			qt.orderBy(key, field, defaultAsc, required);
			return this;
		}
		
		public Using<E> orderBy(String key, String field) {
			return orderBy(key, field, true);
		}
		
		public Using<E> orderByAsc(String key, String field) {
			return orderBy(key, field, true);
		}
		
		public Using<E> orderByDesc(String key, String field) {
			return orderBy(key, field, false);
		}
		public Using<E> relation(String key, View<? extends QueryByKey<?>> view) {
			qt.relation(key, view, required);
			return this;
		}
		public Using<E> valueAdapter(String key, ValueAdapterFactory<E> vaf) {
			qt.valueAdapter(key, vaf, required);
			return this;
		}
	}
	
	protected static class RelationAdapterFactory<E> implements ValueAdapterFactory<E> {
		private final String attribute;
		private final View<? extends QueryByKey<?>> view;
		private final String[] referenceKeys;
		public RelationAdapterFactory(String attribute, View<? extends QueryByKey<?>> view, String[] referenceKeys) {
			this.attribute = attribute;
			this.view = view;
			this.referenceKeys = referenceKeys;
		}
		@Override
		public ValueAdapter<E> newAdapter(Mapping<E> mapping, OConnection cnn, List<String> attributes) {
			return new RelationAdapter<E>(this, cnn, mapping, attributes.toArray(new String[attributes.size()]));
		}
	}
	
	protected static class RelationAdapter<Entity> extends ValueAdapterBase<Entity> {

		private final RelationAdapterFactory<Entity> r;
		private final OConnection cnn;
		private final Mapping<Entity> mapping;
		private final String[] attributes;
		private final List<Entity> records = new ArrayList<>();
		private final List<Object> keys = new ArrayList<>();
		private final IdMap<SQLException> idMap;
		private ResultSet rs = null;
		private int[] keyIndices = null;

		public RelationAdapter(RelationAdapterFactory<Entity> r, OConnection cnn, Mapping<Entity> mapping, String[] attributes) {
			this.r = r;
			this.cnn = cnn;
			this.mapping = mapping;
			this.attributes = attributes;
			idMap = new IdMap<SQLException>(r.referenceKeys.length) {
				@Override
				protected Object[] fetchValues(Object[] keys) throws SQLException {
					return RelationAdapter.this.fetchValues(keys);
				}
			};
		}
		
		@Override
		public void initialize(ResultSet rs) throws SQLException {
			this.rs = rs;
			this.keyIndices = getFieldIndices(rs, r.referenceKeys);
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
			String field = r.attribute;
			for (int i = 0; i < values.length; i++) {
				Entity r = records.get(i);
				mapping.setField(r, field, values[i]);
			}
		}

		protected Object[] fetchValues(Object[] keys) throws SQLException {
			return cnn
					.select(attributes).from(r.view).byKeys(keys)
					.asArray().run();
		}
		
	}

}
