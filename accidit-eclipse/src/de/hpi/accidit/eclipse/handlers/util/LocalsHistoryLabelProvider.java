package de.hpi.accidit.eclipse.handlers.util;

import org.eclipse.jface.viewers.LabelProvider;

import de.hpi.accidit.eclipse.views.dataClasses.LocalBase;

public class LocalsHistoryLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		System.out.println("LocalsHistoryLabelProvider: getText von " + element);
		
		LocalBase local = (LocalBase) element;
		return "" + local.name + ", " + local.type;
	}
	
	
}
