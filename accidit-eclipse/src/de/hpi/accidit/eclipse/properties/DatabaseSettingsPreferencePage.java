package de.hpi.accidit.eclipse.properties;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hpi.accidit.eclipse.Activator;

public class DatabaseSettingsPreferencePage extends FieldEditorOverlayPage
		implements IWorkbenchPreferencePage {
	
	public static final String ID = "de.hpi.accidit.eclipse.preferencePages.DatabaseSettings";
	
	public static final String CONNECTION_ADDRESS	= "NconnectionAddress";
	public static final String CONNECTION_SCHEMA	= "NconnectionSchema";
	public static final String CONNECTION_USER		= "NconnectionUser";
	public static final String CONNECTION_PASSWORD	= "NconnectionPassword";
	public static final String CONNECTION_TYPE		= "NconnectionType";

	public DatabaseSettingsPreferencePage() {
		super(GRID);
	}

	@Override
	public IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	@Override
	public void init(IWorkbench workbench) {
		setDescription("The preference page to specify the database connection parameters.");
	}

	@Override
	protected String getPageId() {
		return ID;
	}

	@Override
	protected void createFieldEditors() {
	    addField(new StringFieldEditor(CONNECTION_ADDRESS, "Address:", getFieldEditorParent()));
	    addField(new StringFieldEditor(CONNECTION_SCHEMA, "Schema:", getFieldEditorParent()));
	    addField(new StringFieldEditor(CONNECTION_USER, "User:", getFieldEditorParent()));
	    addField(new StringFieldEditor(CONNECTION_PASSWORD, "Password:", getFieldEditorParent()));
	    
	    addField(
	    	new RadioGroupFieldEditor(
	    		CONNECTION_TYPE, 
	    		"Database Type:", 
	    		1, 
	    		new String [][] {
	    			{"Orcale MySQL", "mysql"},
	    			{"SAP HANA", "hana"}
	    		},
	    		getFieldEditorParent(), 
	    		true));
	}

}
