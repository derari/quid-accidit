package de.hpi.accidit.eclipse.views.elements;

import org.eclipse.jface.viewers.LabelProvider;

public class LocalsHistoryLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		System.out.println("LocalsHistoryLabelProvider: getText von " + element);
		
		LocalBase local = (LocalBase) element;
		return "" + local.name + ", " + local.type;
	}
	
	
}
