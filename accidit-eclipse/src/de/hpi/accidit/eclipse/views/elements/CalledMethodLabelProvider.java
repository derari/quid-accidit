package de.hpi.accidit.eclipse.views.elements;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class CalledMethodLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if(!(element instanceof CalledMethod)) {
			System.err.println("Invalid Object in tree of class: " + element.getClass().getName());
			return null;
		}			
		
		CalledMethod method = (CalledMethod) element;
		
		switch(columnIndex) {
		case 0: return String.format("%s.%s", method.type, method.method);
		case 1: return String.valueOf(method.callStep);
		case 2: return getFileName(method);
		case 3: return String.valueOf(method.methodId);
		default: return null;
		}
	}
	
	/**
	 * Returns the name of the file the method is called in: example.java:lineNumber
	 * 
	 * @return the file name and the line number
	 */
	public String getFileName(CalledMethod method) {
		if (method.callLine < 1) return "";
		
		//String typeName = method.type.substring(method.type.lastIndexOf(".") + 1);
		return String.format("%s.java:%d", method.parentMethod.type, method.callLine);
	}

}
