package de.hpi.accidit.eclipse.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;
import de.hpi.accidit.orm.dsl.QueryBuilder;
import de.hpi.accidit.orm.dsl.QueryTemplate;
import de.hpi.accidit.orm.dsl.View;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.util.OLazyFuture;
import de.hpi.accidit.orm.util.QueryFactoryView;
import de.hpi.accidit.orm.util.ReflectiveMapping;

public class Invocation extends TraceElement {

	public int testId;
//	public long callStep;
	public long exitStep;
	public int depth;
//	public int callLine;
	
	public boolean returned;
	public int exitLine;
	
	public String type;
	public String method;
	
	private OFuture<TraceElement[]> fChildren = new OLazyFuture<TraceElement[]>() {
		@Override
		protected OFuture<? extends TraceElement[]> initialize() throws Exception {
			final OConnection cnn = DatabaseConnector.cnn();
			return cnn.submit(FETCH_CHILDREN, Invocation.this);
		}
	};

	public Invocation() { };
	
	@Override
	public String getImage() {
		return returned ? "trace_over.png" : "trace_over_fail.png";
	}
	
	@Override
	public String getShortText() {
		return type + " #" + method + "";
	}
	
	public TraceElement[] getChildren() {
		if (fChildren._waitUntilDone()) {
			if (fChildren.hasResult()) {
				return fChildren.getResult();
			} else {
				return new TraceElement[]{new ErrorElement(fChildren.getException().getMessage())};
			}
		}
		return new TraceElement[]{new ErrorElement("interrupted")};
	}
	
	public OFuture<TraceElement[]> asyncChildren() {
		return fChildren;
	}
	
	protected TraceElement[] fetchChildren() throws Exception {
		final OConnection cnn = DatabaseConnector.cnn();
		
		List<Invocation> calls = cnn
				.select().from(VIEW)
				.where().childOf(Invocation.this)
				.asList().run();
		List<ExceptionEvent> catchs = cnn
				.select("line", "step").from(ExceptionEvent.CATCH_VIEW)
				.where().inInvocation(Invocation.this)
				.asList().run();
		List<ExceptionEvent> thrown = cnn
				.select("line", "step").from(ExceptionEvent.CATCH_VIEW)
				.where().inInvocation(Invocation.this)
				.asList().run();
		List<FieldEvent> fields = cnn
				.select("line", "step").from(FieldEvent.PUT_VIEW)
				.where().inInvocation(testId, step)
				.asList().run();
		List<VariableEvent> vars = cnn
				.select("line", "step").from(VariableEvent.VIEW)
				.where().inInvocation(testId, step)
				.asList().run();
		
		SortedSet<TraceElement> major = new TreeSet<>();
		major.addAll(calls);
		major.addAll(catchs);
		major.addAll(thrown);
		
		SortedSet<TraceElement> minor = new TreeSet<>();
		minor.addAll(fields);
		minor.addAll(vars);
		
		List<TraceElement> result = new ArrayList<>();
		
		int line = -1;
		Iterator<TraceElement> mnIt = minor.iterator();
		TraceElement mn = mnIt.hasNext() ? mnIt.next() : null;
		for (TraceElement mj: major) {
//			while (mn != null && mn.line < mj.line) {
//				if (mn.line > line) {
//					result.add(new LineElement(this, mn.step, mn.line));
//					line = mn.line;
//				}
//				while (mn != null && mn.line <= line) {
//					mn = mnIt.hasNext() ? mnIt.next() : null;
//				}
//			}
			result.add(mj);
			line = mj.line;
		}
		while (mn != null) {
			if (mn.line > line) {
				result.add(new LineElement(this, mn.step, mn.line));
				line = mn.line;
			}
			while (mn != null && mn.line <= line) {
				mn = mnIt.hasNext() ? mnIt.next() : null;
			}
		}
		
		result.add(new ExitEvent(this, returned, exitLine, exitStep));
		
		return result.toArray(new TraceElement[result.size()]);
	}

	private static final OFutureAction<Invocation, TraceElement[]> FETCH_CHILDREN = new OFutureAction<Invocation, TraceElement[]>() {
		public TraceElement[] call(Invocation param) throws Exception {
			return param.fetchChildren();
		};
	};
	
	public static final View<Query> VIEW = new QueryFactoryView<>(Query.class);
	
	private static final Mapping<Invocation> MAPPING = new ReflectiveMapping<>(Invocation.class);
	
	private static final QueryTemplate<Invocation> TEMPLATE = new QueryTemplate<Invocation>(){{
		select("testId", 	"i.testId",
			   "step", 		"i.callStep AS step",
			   "exitStep", 	"i.exitStep",
			   "depth",		"i.depth",
			   "line",		"i.callLine AS line",
			   "returned",	"i.returned",
			   "exitLine",	"i.exitLine");
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

	
	public static class Query extends QueryBuilder<Invocation> {
		public Query(OConnection cnn, String[] fields) {
			super(cnn, TEMPLATE, MAPPING);
			select(fields);
		}
		public Query where() {
			return this;
		}
		public Query childOf(Invocation m) {
			where("test_EQ", m.testId);
			where("depth_EQ", m.depth+1);
			where("step_BETWEEN", m.step, m.exitStep);
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
	
}
