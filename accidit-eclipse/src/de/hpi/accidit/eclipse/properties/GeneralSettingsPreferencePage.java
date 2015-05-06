package de.hpi.accidit.eclipse.properties;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class GeneralSettingsPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public GeneralSettingsPreferencePage() {
		super();
	}

	public GeneralSettingsPreferencePage(String title) {
		super(title);
	}

	public GeneralSettingsPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		setDescription("Expand the tree to edit Accidit preferences.");
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		return null;
	}

}
