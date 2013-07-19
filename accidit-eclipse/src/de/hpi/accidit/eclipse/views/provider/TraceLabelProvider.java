package de.hpi.accidit.eclipse.views.provider;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.Pending;
import de.hpi.accidit.eclipse.model.TraceElement;

public class TraceLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	private static final Map<String, Image> images = new HashMap<>();
	
	private static void addImage(String s) {
		Display d = Display.getDefault();
		Image img = new Image(d, TraceLabelProvider.class.getResourceAsStream("/" + s));
		images.put(s, img);
	}
	
	static {
		addImage("trace_line.png");
		addImage("trace_over.png");
		addImage("trace_over_fail.png");
		addImage("trace_return.png");
		addImage("trace_fail.png");
		addImage("trace_catch.png");
		addImage("trace_throw.png");
	}
	
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0 && element instanceof TraceElement) {
			return images.get(((TraceElement) element).getImage());
		}
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof Pending) {
			if (columnIndex == 0) return "Pending...";
			return "";
		}
		
		if (element instanceof String) {
			if (columnIndex == 0) return element.toString();
			return "";
		}
		
		if (columnIndex == 1 && element instanceof TraceElement) {
			return String.valueOf(((TraceElement) element).step);
		}
		
		if (element instanceof TraceElement) {
			TraceElement le = (TraceElement) element;
			switch(columnIndex) {
			case 0: return le.getShortText();
			case 1: return String.valueOf(le.step);
			default: return "";
			}
		}
		
		if(!(element instanceof Invocation)) {
			return "";
		}			
		
		Invocation method = (Invocation) element;
		
		switch(columnIndex) {
		case 0: return String.format("%s.%s", method.type, method.method);
		case 2: return getFileName(method);
		case 3: return String.valueOf(0);
		default: return null;
		}
	}
	
	/**
	 * Returns the name of the file the method is called in: example.java:lineNumber
	 * 
	 * @return the file name and the line number
	 */
	public String getFileName(Invocation method) {
		if (method.line < 1) return "";
		
		//String typeName = method.type.substring(method.type.lastIndexOf(".") + 1);
		return String.format("%s.java:%d", method.parent.type, method.line);
	}

}
