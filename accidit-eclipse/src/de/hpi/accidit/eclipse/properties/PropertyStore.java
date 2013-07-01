package de.hpi.accidit.eclipse.properties;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;

public class PropertyStore extends PreferenceStore {
	
	private IResource resource;
	private IPreferenceStore workbenchStore;
	private String pageId;
	
	/** Semaphore to avoid unlimited recursion in insertValue() */
	private boolean inserting = false;
	
	public PropertyStore(IResource resource, IPreferenceStore workbenchStore, String pageId) {
		this.resource = resource;
		this.workbenchStore = workbenchStore;
		this.pageId = pageId;
	}
	
	/* Write modified values to properties */

	@Override
	public void save() throws IOException {
		writeProperties();
	}

	@Override
	public void save(OutputStream out, String header) throws IOException {
		writeProperties();
	}

	private void writeProperties() throws IOException {
		String[] preferenceNames = super.preferenceNames();
		for (String name : preferenceNames) {
			try {
				setProperty(name, getString(name));
			} catch (CoreException e) {
				throw new IOException("Can not write resource properts" + name);
			}
		}
	}

	private void setProperty(String name, String value) throws CoreException {
		resource.setPersistentProperty(new QualifiedName(pageId, name), value);
	}
	
	/* Get default property values. */
	
	@Override
	public float getDefaultFloat(String name) {
		return workbenchStore.getDefaultFloat(name);
	}
	
	@Override
	public String getDefaultString(String name) {
		return workbenchStore.getDefaultString(name);
	}
	
	@Override
	public boolean getDefaultBoolean(String name) {
		return workbenchStore.getDefaultBoolean(name);
	}
	
	@Override
	public double getDefaultDouble(String name) {
		return workbenchStore.getDefaultDouble(name);
	}
	
	@Override
	public int getDefaultInt(String name) {
		return workbenchStore.getDefaultInt(name);
	}
	
	@Override
	public long getDefaultLong(String name) {
		return workbenchStore.getDefaultLong(name);
	}
	
	/* Get property values. */
	
	public boolean getBoolean(String name) {
		insertValue(name);
		return super.getBoolean(name);
	}

	@Override
	public double getDouble(String name) {
		insertValue(name);
		return super.getDouble(name);
	}

	@Override
	public float getFloat(String name) {
		insertValue(name);
		return super.getFloat(name);
	}

	@Override
	public int getInt(String name) {
		insertValue(name);
		return super.getInt(name);
	}

	@Override
	public long getLong(String name) {
		insertValue(name);
		return super.getLong(name);
	}

	@Override
	public String getString(String name) {
		insertValue(name);
		return super.getString(name);
	}

	private synchronized void insertValue(String name) {
		if (inserting) return;
		if (super.contains(name)) return;
		
		inserting = true;
		String prop = null;
		try {
			prop = getProperty(name);
		} catch (CoreException e) { }
		if (prop == null) prop = workbenchStore.getString(name);
		if (prop != null) setValue(name, prop);
		inserting = false;
	}

	private String getProperty(String name) throws CoreException {
		return resource.getPersistentProperty(new QualifiedName(pageId, name));
	}
	
	/* Other required methods. */

	@Override
	public boolean contains(String name) {
		return workbenchStore.contains(name);
	}

	@Override
	public void setToDefault(String name) {
		setValue(name, getDefaultString(name));
	}

	@Override
	public boolean isDefault(String name) {
		String defaultValue = getDefaultString(name);
		if (defaultValue == null) return false;
		return defaultValue.equals(getString(name));
	}
	

}
