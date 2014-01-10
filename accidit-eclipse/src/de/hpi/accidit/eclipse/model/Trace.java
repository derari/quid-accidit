package de.hpi.accidit.eclipse.model;

import static org.cthul.miro.DSL.*;
import de.hpi.accidit.eclipse.TraceNavigatorUI;

public class Trace {

	public final int id;
	public final TraceNavigatorUI ui;

	public TraceElement[] root;
	
	public Trace(int id, final TraceNavigatorUI ui) {
		this.id = id;
		this.ui = ui;
		try {
			root = select()
				.from(Invocation.VIEW)
				.where().rootOfTest(id)
				.execute(ui.cnn()).asArray();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			root = new TraceElement[0];
		}
	}
	
	public TraceElement[] getRootElements() {
		return root;
	}
}
