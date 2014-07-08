package de.hpi.accidit.eclipse.properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class GeneralSettingsPropertyPage extends PropertyPage implements
		IWorkbenchPropertyPage {

	public GeneralSettingsPropertyPage() {
		setDescription("Expand the tree to edit Accidit properties.");
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		return null;
	}

}
