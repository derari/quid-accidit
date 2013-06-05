package de.hpi.accidit.eclipse.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class DatabaseInformation extends PropertyPage implements
		IWorkbenchPropertyPage {

	public DatabaseInformation() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Control createContents(Composite parent) {
		new Label(parent, SWT.NONE).setText("Hi there!");
		
		return null;
	}

}
