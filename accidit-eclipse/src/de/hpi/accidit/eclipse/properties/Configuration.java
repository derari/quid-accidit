package de.hpi.accidit.eclipse.properties;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.hpi.accidit.eclipse.Activator;

public class Configuration extends AbstractPreferenceInitializer {
	
	public static final String CONNECTION_ADDRESS	= "NconnectionAddress";
	public static final String CONNECTION_SCHEMA	= "NconnectionSchema";
	public static final String CONNECTION_USER		= "NconnectionUser";
	public static final String CONNECTION_PASSWORD	= "NconnectionPassword";
	public static final String CONNECTION_TYPE		= "NconnectionType";
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(CONNECTION_ADDRESS,	"localhost");
		store.setDefault(CONNECTION_SCHEMA,		"Accidit");
		store.setDefault(CONNECTION_USER,		"root");
		store.setDefault(CONNECTION_PASSWORD,	"");
		store.setDefault(CONNECTION_TYPE,		"mysql");
	}

}
