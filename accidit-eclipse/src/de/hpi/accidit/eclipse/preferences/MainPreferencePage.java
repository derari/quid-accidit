package de.hpi.accidit.eclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.hpi.accidit.eclipse.Activator;
import de.hpi.accidit.eclipse.DatabaseConnector;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class MainPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	// TODO Add test connection button.
	// TODO Use dedicated password field for the password preference.
	
	private StringFieldEditor dbAddressField;
	private StringFieldEditor dbSchemaField;
	private StringFieldEditor dbUserField;
	private StringFieldEditor dbPasswordField;
	
	public MainPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("The preference page to specify the database connection parameters.");
	}

	@Override
	public void init(IWorkbench workbench) { }
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		dbAddressField	= new StringFieldEditor(PreferenceConstants.CONNECTION_ADDRESS,		"Address: ",	getFieldEditorParent());
		dbSchemaField	= new StringFieldEditor(PreferenceConstants.CONNECTION_SCHEMA,		"Schema: ",		getFieldEditorParent());
		dbUserField		= new StringFieldEditor(PreferenceConstants.CONNECTION_USER,		"User: ",		getFieldEditorParent());
		dbPasswordField	= new StringFieldEditor(PreferenceConstants.CONNECTION_PASSWORD,	"Password: ",	getFieldEditorParent());
		
		addField(dbAddressField);
		addField(dbSchemaField);
		addField(dbUserField);
		addField(dbPasswordField);
	}
	
	public boolean performOk() {
		boolean connectionIsWorking = DatabaseConnector.testConnection(
				dbAddressField.getStringValue(), 
				dbSchemaField.getStringValue(), 
				dbUserField.getStringValue(), 
				dbPasswordField.getStringValue());
		
		String errorMessage = connectionIsWorking ? null : "This database does not exist or is not available.";
		setErrorMessage(errorMessage);
		return super.performOk() && connectionIsWorking;
	}
	
	
	
}