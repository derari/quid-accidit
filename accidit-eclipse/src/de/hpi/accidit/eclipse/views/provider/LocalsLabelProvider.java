package de.hpi.accidit.eclipse.views.provider;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.dataClasses.LocalBase;

public class LocalsLabelProvider extends LabelProvider implements
		ITableLabelProvider, ITableColorProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}
	
	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof NamedValue)) {
			return String.valueOf(element);
		}
		NamedValue nv = (NamedValue) element;
		switch (columnIndex) {
		case 0: return nv.getName();
		case 1: 
			if (nv.isInitialized()) {
				return nv.getValue().getLongString();
			} else {
				return "Pending...";
			}
		}
		return "-";
	}

	@Override
	public Color getForeground(Object element, int columnIndex) {
		if (!(element instanceof NamedValue)) {
			return null;
		}
		NamedValue nv = (NamedValue) element;
		if (!nv.isActiveValue()) {
			return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
		}
		return null;
	}

	@Override
	public Color getBackground(Object element, int columnIndex) {
		return null;
	}

}
