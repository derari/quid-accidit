package de.hpi.accidit.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import de.hpi.accidit.eclipse.Activator;


/**
 * Convenience class to retrieve properties from resources. 
 * If the resource is null or does not exist, the respective property key is used to get the default preference from the preference store.
 */
public class DatabaseSettingsRetriever {
	
	public static final String CONNECTION_ADDRESS	= DatabaseSettingsPreferencePage.CONNECTION_ADDRESS;
	public static final String CONNECTION_SCHEMA	= DatabaseSettingsPreferencePage.CONNECTION_SCHEMA;
	public static final String CONNECTION_USER		= DatabaseSettingsPreferencePage.CONNECTION_USER;
	public static final String CONNECTION_PASSWORD	= DatabaseSettingsPreferencePage.CONNECTION_PASSWORD;
	public static final String CONNECTION_TYPE		= DatabaseSettingsPreferencePage.CONNECTION_TYPE;
	
	public static String getPreferenceValue(IResource resource, String key) {
		if (resource == null) {
			return Activator.getDefault().getPreferenceStore().getString(key);
		}
		
		IProject project = resource.getProject();
		String value = null;
		if (useProjectSettings(project, DatabaseSettingsPreferencePage.ID)) {
			value = getProperty(resource, DatabaseSettingsPreferencePage.ID, key);
		}
		if (value != null)
			return value;
		return Activator.getDefault().getPreferenceStore().getString(key);
	}
	
	private static boolean useProjectSettings(IResource resource, String pageId) {
		String use = getProperty(resource, pageId, FieldEditorOverlayPage.USEPROJECTSETTINGS);
		return "true".equals(use);
	}
	
	private static String getProperty(IResource resource, String pageId, String key) {
		try {
			return resource.getPersistentProperty(new QualifiedName(pageId, key));
		} catch (CoreException e) { }
		return null;
	}

}
