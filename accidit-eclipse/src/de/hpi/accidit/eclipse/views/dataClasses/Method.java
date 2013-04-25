package de.hpi.accidit.eclipse.views.dataClasses;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.dsl.QueryBuilder;
import de.hpi.accidit.orm.dsl.QueryTemplate;
import de.hpi.accidit.orm.dsl.View;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.ResultBuilder;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;
import de.hpi.accidit.orm.util.QueryFactoryView;
import de.hpi.accidit.orm.util.ReflectiveMapping;


public class Method {

	public int testId;
	public long callStep;
	public long exitStep;
	public int depth;
	public int callLine;
	
	public int methodId;
	public String type;
	public String method;
	
	public Method parentMethod;
	public Method[] children;

	public Method() { };
	
	public static final View<Query> VIEW = new QueryFactoryView<>(Query.class);
	
	private static final Mapping<Method> MAPPING = new ReflectiveMapping<>(Method.class);
	
	private static final QueryTemplate<Method> TEMPLATE = new QueryTemplate<Method>(){{
		select("testId", 	"i.testId",
			   "callStep", 	"i.callStep",
			   "exitStep", 	"i.exitStep",
			   "depth",		"i.depth",
			   "callLine",	"i.callLine");
		from("InvocationTrace i");
		optional_join("m", "Method m", "i.methodId = m.id");
		using("m")
			.select("method", "m.name AS method")
			.optional_join("t", "Type t", "m.declaringTypeId = t.id");
		using("t")
			.select("type", "t.name AS type");
		
		where("test_EQ", "i.testId = ?",
			  "depth_EQ", "i.depth = ?",
			  "step_BETWEEN", "i.callStep > ? AND i.callStep < ?");
		
		orderBy("o_callStep", "callStep");
	}};

	
	public static class Query extends QueryBuilder<Method> {
		public Query(OConnection cnn, String[] fields) {
			super(cnn, TEMPLATE, MAPPING);
			select(fields);
		}
		public Query where() {
			return this;
		}
		public Query childOf(Method m) {
			where("test_EQ", m.testId);
			where("depth_EQ", m.depth+1);
			where("step_BETWEEN", m.callStep, m.exitStep);
			orderBy("o_callStep");
			apply(new SetParentAdapter(m));
			return this;
		}
		public Query rootOfTest(int i) {
			where("test_EQ", i);
			where("depth_EQ", 0);
			return this;
		}
	}
	
	private static class SetParentAdapter 
					implements ResultBuilder.ValueAdapter<Method>, 
							   ResultBuilder.ValueAdapterFactory<Method> {
		
		private final Method parent;
		
		public SetParentAdapter(Method parent) {
			this.parent = parent;
		}

		@Override
		public ValueAdapter<Method> newAdapter(Mapping<Method> mapping,
				OConnection cnn, List<String> attributes) {
			return this;
		}

		@Override
		public void initialize(ResultSet rs) throws SQLException {}

		@Override
		public void apply(Method entity) throws SQLException {
			entity.parentMethod = parent;
		}

		@Override
		public void complete() throws SQLException {}
	}
}
