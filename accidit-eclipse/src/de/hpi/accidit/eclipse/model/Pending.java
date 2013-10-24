package de.hpi.accidit.eclipse.model;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.hpi.accidit.eclipse.views.TraceExplorerView.TraceLabelProvider;

public class Pending extends TraceElement {
	
	public static final Image imgWait;
	
	static {
		Image i;
		try {
			Display d = Display.getDefault();
			i = new Image(d, Pending.class.getResourceAsStream("/wait.png"));
		} catch (Exception e) {
			i = null;
			e.printStackTrace(System.err);
		}
		imgWait = i;
	}
	
}
