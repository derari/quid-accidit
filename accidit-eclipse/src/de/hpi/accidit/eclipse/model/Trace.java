package de.hpi.accidit.eclipse.model;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;

public class Trace {

	public final int id;
	public final TraceNavigatorUI ui;

	public TraceElement[] root;
	
	public Trace(int id, final TraceNavigatorUI ui) {
		this.id = id;
		this.ui = ui;
		root = ui.cnn()
			.select()
			.from(Invocation.VIEW)
			.where().rootOfTest(id)
			.asArray()._run();
	}
	
}
