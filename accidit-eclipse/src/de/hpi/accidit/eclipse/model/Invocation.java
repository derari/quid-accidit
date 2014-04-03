package de.hpi.accidit.eclipse.model;

import static org.cthul.miro.DSL.select;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cthul.miro.view.ViewR;

import de.hpi.accidit.eclipse.model.db.InvocationDao;
import de.hpi.accidit.eclipse.model.db.InvocationDao.Query;
import de.hpi.accidit.eclipse.model.db.TraceElementDaoBase;

public class Invocation extends TraceElement {
	
	public static final ViewR<Query> VIEW = InvocationDao.VIEW;

	public long exitStep = -1;
	public int depth = -1;
	
	public boolean returned;
	public int exitLine = -1;
	
	public int methodId = -1;
	public String type;
	public String method;
	public String signature;
	
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
	
	private <T extends TraceElement, Q extends TraceElementDaoBase.Query<T, Q>> List<T> selectEvents(ViewR<Q> view) throws SQLException {
		return select("line", "step").from(view)
				.where().inInvocation(this)
				.orderBy().step_asc()
				.execute(cnn()).asList();
	}
	
	@Override
	protected void lazyInitialize() throws Exception {
		List<Invocation> calls = select().from(VIEW)
				.where().inInvocation(Invocation.this)
				.execute(cnn()).asList();
		List<ExceptionEvent> catchs = selectEvents(ExceptionEvent.CATCH_VIEW);
		List<ExceptionEvent> thrown = selectEvents(ExceptionEvent.THROW_VIEW);
		List<FieldEvent> fields = selectEvents(FieldEvent.PUT_VIEW);
		List<VariableEvent> vars = selectEvents(VariableEvent.PUT_VIEW);
		
		SortedSet<TraceElement> major = new TreeSet<>();
		major.addAll(calls);
		major.addAll(catchs);
		major.addAll(thrown);
		
		SortedSet<TraceElement> minor = new TreeSet<>();
		minor.addAll(fields);
		minor.addAll(vars);
		
		List<TraceElement> result = new ArrayList<>();
		
		// aggregate minors to line events
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
}
