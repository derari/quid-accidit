package de.hpi.accidit.eclipse.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cthul.miro.MiConnection;
import org.cthul.miro.at.AnnotatedQueryHandler;
import org.cthul.miro.at.AnnotatedQueryTemplate;
import org.cthul.miro.at.AnnotatedView;
import org.cthul.miro.at.Impl;
import org.cthul.miro.dsl.View;
import org.cthul.miro.map.MappedTemplateQuery;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.map.ReflectiveMapping;

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
	
	private <T extends TraceElement, Q extends TraceElement.Query<T, Q>> List<T> selectEvents(MiConnection cnn, View<Q> view) throws SQLException {
		return cnn.select("line", "step").from(view)
				.where().inInvocation(this)
				.orderBy().step_asc()
				.asList().execute();
	}
	
	@Override
	protected void lazyInitialize() throws Exception {
		final MiConnection cnn = DatabaseConnector.cnn();
		
		List<Invocation> calls = cnn
				.select().from(VIEW)
				.where().inInvocation(Invocation.this)
				.asList().execute();
		List<ExceptionEvent> catchs = selectEvents(cnn, ExceptionEvent.CATCH);
		List<ExceptionEvent> thrown = selectEvents(cnn, ExceptionEvent.THROW);
		List<FieldEvent> fields = selectEvents(cnn, FieldEvent.PUT);
		List<VariableEvent> vars = selectEvents(cnn, VariableEvent.PUT);
		
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
	
	private static final Mapping<Invocation> MAPPING = new ReflectiveMapping<Invocation>(Invocation.class) {
		protected void injectField(Invocation record, String field, java.sql.ResultSet rs, int i) throws java.sql.SQLException {
			if (field.equals("returned")) {
				injectField(record, field, rs.getInt(i) == 1);
				return;
			}
			super.injectField(record, field, rs, i);
		};
	};
	
	private static final AnnotatedQueryTemplate<Invocation> TEMPLATE = new AnnotatedQueryTemplate<Invocation>(){{
		select("e.`testId`", "e.`depth`", 
			   "x.`step` AS `exitStep`", "x.`returned`", "x.`line` AS `exitLine`",
			   "m.`name` AS `method`", "t.`name` AS `type`");
		from("`CallTrace` e");
		join("LEFT OUTER JOIN `ExitTrace` x ON e.`testId` = x.`testId` AND e.`step` = x.`callStep`");
		join("`Method` m ON e.`methodId` = m.`id`");
		using("m")
			.join("`Type` t ON m.`declaringTypeId` = t.`id`");
		
		where("test_EQ", "e.`testId` = ?",
			  "depth_EQ", "e.`depth` = ?",
			  "step_BETWEEN", "e.`step` > ? AND e.`step` < ?");
	}};
	
	public static final View<Query> VIEW = new AnnotatedView<>(Query.class, MAPPING, TEMPLATE);
	
	@Impl(QueryImpl.class)
	public static interface Query extends TraceElement.Query<Invocation, Query> {
		
		Query inInvocation(Invocation inv);
		
		Query rootOfTest(int i);
	}
	
	static class QueryImpl {
		
		public static void inInvocation(AnnotatedQueryHandler<Invocation> query, Invocation inv) {
			query.configure(new InitParent(inv));
			query.put("testId_EQ", inv.testId);
			query.put("depth_EQ", inv.depth+1);
			query.put("step_BETWEEN", inv.step, inv.exitStep);
			query.put("asc_step");
		}
		
		public static void rootOfTest(AnnotatedQueryHandler<Invocation> query, int testId) {
			query.put("testId_EQ", testId);
			query.put("depth_EQ", 0);
		}
	}

//
//	
//	public static class Query extends QueryWithTemplate<Invocation> {
//		public Query(MiConnection cnn, String[] fields) {
//			super(cnn, MAPPING, TEMPLATE);
//			select(fields);
//		}
//		public Query where() {
//			return this;
//		}
//		public Query childOf(Invocation m) {
//			where("test_EQ", m.testId);
//			where("depth_EQ", m.depth+1);
//			where("step_BETWEEN", m.step, m.exitStep);
//			orderBy("o_callStep");
//			adapter(new InitParent(m));
//			return this;
//		}
//		public Query rootOfTest(int i) {
//			where("test_EQ", i);
//			where("depth_EQ", 0);
//			return this;
//		}
//	}
//	
}
