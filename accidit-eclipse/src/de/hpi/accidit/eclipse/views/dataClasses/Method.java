package de.hpi.accidit.eclipse.views.dataClasses;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.dsl.QueryBuilder;
import de.hpi.accidit.orm.dsl.QueryTemplate;
import de.hpi.accidit.orm.dsl.View;
import de.hpi.accidit.orm.map.Mapping;


public class Method {

	public int testId;
	public long callStep;
	public int exitStep;
	public int depth;
	public int callLine;
	
	public int methodId;
	public String type;
	public String method;
	
	public Method parentMethod;

	public Method() { };
	
	public static final View<Query> VIEW = new View<Method.Query>() {
		@Override
		public Query newQuery(OConnection cnn, String[] select) {
			return new Query(cnn, select);
		}
	};
	
	private static final Mapping<Method> MAPPING = new Mapping<Method>(Method.class) {
		protected Method newRecord() {
			return new Method();
		};
		protected void setField(Method record, String field, java.sql.ResultSet rs, int i) throws java.sql.SQLException {
			injectField(record, field, rs, i);
		};
	};
	
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
		
		where("test_EQ", "i.test = ?",
			  "depth_EQ", "i.depth = ?",
			  "step_BETWEEN", "i.callStep > ? AND i.callStep < =");
		
		orderBy("o_callStep", "callStep");
	}};

	
	public static class Query extends QueryBuilder<Method> {
		public Query(OConnection cnn, String[] fields) {
			super(cnn, TEMPLATE, MAPPING);
			select(fields);
		}
		public Query childrenOfCall(Method m) {
			where("test_EQ", m.testId);
			where("depth_EQ", m.depth+1);
			where("step_BETWEEN", m.callStep, m.exitStep);
			orderBy("o_callStep");
			return this;
		}
		@Override
		protected String queryString() {
			String s = super.queryString();
			System.out.println(s);
			return s;
		}
	}
}
