package de.hpi.accidit.eclipse.handlers.util;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;


public class TestCaseSelectionLabelProvider extends LabelProvider implements
	ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if(!(element instanceof TestCase)) {
			System.err.println("Invalid Object in tree of class: " + element.getClass().getName());
			return null;
		}			
		
		TestCase testCase = (TestCase) element;
		switch(columnIndex) {
		case 0: return String.valueOf(testCase.id);
		case 1: return testCase.name;
		default: return null;
		}
	}

}
