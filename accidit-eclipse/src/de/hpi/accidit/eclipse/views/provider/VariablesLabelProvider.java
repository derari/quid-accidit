package de.hpi.accidit.eclipse.views.provider;

import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.provider.ThreadsafeContentProvider.ContentNode;

public class VariablesLabelProvider extends LabelProvider implements
		ITableLabelProvider, ITableColorProvider {
	
	@Override
	public boolean isLabelProperty(Object element, String property) {
		return "label".equals(property);
	}
	
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
//		if (element instanceof ContentNode && columnIndex == 0) {
//			ContentNode cn = (ContentNode) element;
//			if (cn.isInvalid()) {
//				return Pending.imgWait;
//			}
//		}
		return null;
	}
	
	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof ContentNode) {
			ContentNode cn = (ContentNode) element;
			element = cn.getNodeValue();
		}
		if (!(element instanceof NamedValue)) {
			System.out.println(":(" + element);
			return String.valueOf(element);
		}
		NamedValue nv = (NamedValue) element;
		switch (columnIndex) {
		case 0: 
			System.out.println("." + nv.getName());
			return nv.getName();
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
		if (element instanceof ContentNode) {
			element = ((ContentNode) element).getNodeValue();
		}
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
