package de.hpi.accidit.eclipse.views.elements;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class CalledMethodLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		// TODO Auto-generated method stub
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
		case 0: return method.methodName;
		case 1: return String.valueOf(method.callLine); 
		default: return null;
		}
	}

}
