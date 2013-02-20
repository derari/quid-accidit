package de.hpi.accidit.eclipse.views.elements;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class LocalsLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if(!(element instanceof LocalBase)) {
			System.err.println("Invalid Object in tree of class: " + element.getClass().getName());
			return null;
		}			
		
		LocalBase local = (LocalBase) element;
		switch(columnIndex) {
		case 0: return local.getName();
		case 1: return local.getValue();
		case 2: return local.getType();
		case 3: return local.getStep();
		default: return null;
		}
	}

}
