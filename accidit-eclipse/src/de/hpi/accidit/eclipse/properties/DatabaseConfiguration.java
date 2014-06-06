package de.hpi.accidit.eclipse.properties;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.hpi.accidit.eclipse.Activator;

public class DatabaseConfiguration extends AbstractPreferenceInitializer {
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(DatabaseSettingsPreferencePage.CONNECTION_ADDRESS,	 "localhost");
		store.setDefault(DatabaseSettingsPreferencePage.CONNECTION_SCHEMA,	 "Accidit");
		store.setDefault(DatabaseSettingsPreferencePage.CONNECTION_USER,	 "root");
		store.setDefault(DatabaseSettingsPreferencePage.CONNECTION_PASSWORD, "");
		store.setDefault(DatabaseSettingsPreferencePage.CONNECTION_TYPE,	 "mysql");
	}

}
