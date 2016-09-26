package de.hpi.accidit.eclipse.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import de.hpi.accidit.eclipse.model.db.TraceElementDaoBase;

public class Invocation extends TraceElement {

	public Long thisId;
	
	public long exitStep = -1;
	public int depth = -1;
	
	public boolean returned;
	public int exitLine = -1;
	
	public int methodId = -1;
	public String type;
	public String method;
	public String signature;
	
	private TraceElement[] children = null;
	private Method methodObject = null;
	
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
	
	private <T extends TraceElement, Dao extends TraceElementDaoBase<T, Dao>> List<T> selectEvents(Dao dao) {
		try {
			return dao.inInvocation(this)
					.orderByStep()
					.result().asList();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Collections.emptyList();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected void lazyInitialize() throws Exception {
		List<Invocation> calls = selectEvents(db().invocations());
		
		List<ExceptionEvent> catchs = selectEvents(db().catchEvents());
		List<ExceptionEvent> thrown = selectEvents(db().throwEvents());
		List<FieldEvent> fields = selectEvents(db().fieldEvents());
		List<VariableEvent> vars = selectEvents(db().variableEvents());
		
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
			while (mn != null && mn.step < mj.step) {
				if (mn.line != line) {
					result.add(new LineElement(this, mn.step, mn.line));
					line = mn.line;
				}
				mn = mnIt.hasNext() ? mnIt.next() : null;
			}
			result.add(mj);
			line = mj.line;
		}
		while (mn != null) {
			if (mn.line != line) {
				result.add(new LineElement(this, mn.step, mn.line));
				line = mn.line;
			}
			while (mn != null && mn.line == line) {
				mn = mnIt.hasNext() ? mnIt.next() : null;
			}
		}
		
		result.add(new ExitEvent(this, returned, exitLine, exitStep));
		
		children = result.toArray(new TraceElement[result.size()]);
	}

	public Method quickGetMethod() {
		if (methodObject == null) {
			methodObject = db().methods().
					byId(methodId)
					.result()._getSingle();
		}
		return methodObject;
	}
}
