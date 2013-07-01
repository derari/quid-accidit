package de.hpi.accidit.eclipse.properties;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hpi.accidit.eclipse.Activator;

public class DatabaseSettingsPreferencePage extends FieldEditorOverlayPage
		implements IWorkbenchPreferencePage {
	
	private static final String ID = "de.hpi.accidit.eclipse.preferencePages.DatabaseSettings";
	
	public static final String SOME_PREFERENCE_CONSTANT = "someNameOfAPref";
	public static final String INT_PREFERENCE_CONSTANT = "someIntNameOfAPref";

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
	    addField(new StringFieldEditor(Configuration.CONNECTION_ADDRESS, "Address:", getFieldEditorParent()));
	    addField(new StringFieldEditor(Configuration.CONNECTION_SCHEMA, "Schema:", getFieldEditorParent()));
	    addField(new StringFieldEditor(Configuration.CONNECTION_USER, "User:", getFieldEditorParent()));
	    addField(new StringFieldEditor(Configuration.CONNECTION_PASSWORD, "Password:", getFieldEditorParent()));
	    
	    addField(
	    	new RadioGroupFieldEditor(
	    		Configuration.CONNECTION_TYPE, 
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
