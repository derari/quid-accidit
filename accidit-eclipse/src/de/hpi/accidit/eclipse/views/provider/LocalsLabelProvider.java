package de.hpi.accidit.eclipse.views.provider;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.dataClasses.LocalBase;

public class LocalsLabelProvider extends LabelProvider implements
		ITableLabelProvider {

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

}
