package de.hpi.accidit.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.hpi.accidit.eclipse.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.CONNECTION_ADDRESS, "localhost");
		store.setDefault(PreferenceConstants.CONNECTION_SCHEMA, "Accidit");
		store.setDefault(PreferenceConstants.CONNECTION_USER, "root");
		store.setDefault(PreferenceConstants.CONNECTION_PASSWORD, "");
	}

}
