package de.hpi.accidit.eclipse.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cthul.miro.MiConnection;
import org.cthul.miro.MiFuture;
import org.cthul.miro.MiFutureAction;
import org.cthul.miro.dsl.QueryBuilder;
import org.cthul.miro.dsl.QueryTemplate;
import org.cthul.miro.dsl.QueryWithTemplate;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.util.LazyFuture;
import org.cthul.miro.util.QueryFactoryView;
import org.cthul.miro.util.ReflectiveMapping;

import de.hpi.accidit.eclipse.DatabaseConnector;

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
	
	private TraceElement[] children = null;

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
		if (!beInitialized()) {
			return new TraceElement[]{new ErrorElement("interrupted")};
		}
		if (!isInitSuccess()) {
			return new TraceElement[]{new ErrorElement(getInitException().getMessage())};
		}
		return children;
	}
	
	@Override
	protected void lazyInitialize() throws Exception {
		final MiConnection cnn = DatabaseConnector.cnn();
		
		List<Invocation> calls = cnn
				.select().from(VIEW)
				.where().childOf(Invocation.this)
				.asList().execute();
		List<ExceptionEvent> catchs = cnn
				.select("line", "step").from(ExceptionEvent.CATCH_VIEW)
				.where().inInvocation(Invocation.this)
				.asList().execute();
		List<ExceptionEvent> thrown = cnn
				.select("line", "step").from(ExceptionEvent.THROW_VIEW)
				.where().inInvocation(Invocation.this)
				.asList().execute();
		List<FieldEvent> fields = cnn
				.select("line", "step").from(FieldEvent.PUT_VIEW)
				.where().inInvocation(testId, step)
				.asList().execute();
		List<VariableEvent> vars = cnn
				.select("line", "step").from(VariableEvent.VIEW)
				.where().inInvocation(testId, step)
				.asList().execute();
		
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
			while (mn != null && mn.line < mj.line) {
				if (mn.line > line) {
					result.add(new LineElement(this, mn.step, mn.line));
					line = mn.line;
				}
				while (mn != null && mn.line <= line) {
					mn = mnIt.hasNext() ? mnIt.next() : null;
				}
			}
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
		
		children = result.toArray(new TraceElement[result.size()]);
	}
	
	public static final View<Query> VIEW = new QueryFactoryView<>(Query.class);
	
	private static final Mapping<Invocation> MAPPING = new ReflectiveMapping<Invocation>(Invocation.class) {
		protected void injectField(Invocation record, String field, java.sql.ResultSet rs, int i) throws java.sql.SQLException {
			if (field.equals("returned")) {
				injectField(record, field, rs.getInt(i) == 1);
				return;
			}
			super.injectField(record, field, rs, i);
		};
	};
	
	private static final QueryTemplate<Invocation> TEMPLATE = new QueryTemplate<Invocation>(){{
		select("c.`testId`", "c.`step` AS `step`", "e.`step` AS `exitStep`", 
			   "c.`depth`", "c.`line` AS `line`",
			   "e.`returned`", "e.`line` AS `exitLine`",
			   "m.`name` AS `method`", "t.`name` AS `type`");
		from("`CallTrace` c");
		join("LEFT OUTER JOIN `ExitTrace` e ON c.`testId` = e.`testId` AND c.`step` = e.`callStep`");
		join("`Method` m ON c.`methodId` = m.`id`");
		using("m")
			.join("`Type` t ON m.`declaringTypeId` = t.`id`");
		
		where("test_EQ", "c.`testId` = ?",
			  "depth_EQ", "c.`depth` = ?",
			  "step_BETWEEN", "c.`step` > ? AND c.`step` < ?");
		
		orderBy("o_callStep", "`callStep`");
	}};

	
	public static class Query extends QueryWithTemplate<Invocation> {
		public Query(MiConnection cnn, String[] fields) {
			super(cnn, MAPPING, TEMPLATE);
			select_keys(fields);
		}
		public Query where() {
			return this;
		}
		public Query childOf(Invocation m) {
			where_key("test_EQ", m.testId);
			where_key("depth_EQ", m.depth+1);
			where_key("step_BETWEEN", m.step, m.exitStep);
			orderBy_key("o_callStep");
			adapter(new SetParentAdapter(m));
			return this;
		}
		public Query rootOfTest(int i) {
			where_key("test_EQ", i);
			where_key("depth_EQ", 0);
			return this;
		}
	}
	
}
