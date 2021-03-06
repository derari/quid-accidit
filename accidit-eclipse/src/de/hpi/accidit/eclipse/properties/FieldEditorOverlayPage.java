package de.hpi.accidit.eclipse.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;

public abstract class FieldEditorOverlayPage extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {
	
	/** Returns the id of the current preference page. (Id as defined in plugin.xml). */
	protected abstract String getPageId();

	/** Name of resource property for the selection of workbench or project settings */
	public static final String USEPROJECTSETTINGS = "useProjectSettings";

	private static final String FALSE = "false";
	private static final String TRUE = "true";
	
	private List<FieldEditor> fieldEditors = new ArrayList<FieldEditor>();
	
	private String pageId;
	private IPreferenceStore overlayStore;

	/** The element that owns the properties of this preference page. */
	private IAdaptable element;
	
	private Button useWorkspaceSettingsButton, useProjectSettingsButton, configureButton;

	/** ImageDescriptor for the title image of this page. */
	private ImageDescriptor image;

	public FieldEditorOverlayPage(int style) {
		super(style);
	}

	public FieldEditorOverlayPage(String title, int style) {
		super(title, style);
	}

	public FieldEditorOverlayPage(String title, ImageDescriptor image, int style) {
		super(title, image, style);
		this.image = image;
	}

	@Override
	public void setElement(IAdaptable element) {
		this.element = element;
	}

	@Override
	public IAdaptable getElement() {
		return element;
	}

	public boolean isPropertyPage() {
		return getElement() != null;
	}

	@Override
	protected void addField(FieldEditor editor) {
		fieldEditors.add(editor);
		super.addField(editor);
	}
	
	@Override
	public void createControl(Composite parent) {
		if (isPropertyPage()) {
			pageId = getPageId();
			overlayStore = new PropertyStore((IResource) getElement(), super.getPreferenceStore(), pageId);
		}
		
		super.createControl(parent);
		if (isPropertyPage()) {
			updateFieldEditors();
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		if (isPropertyPage()) {
			createSelectionGroup(parent);
		}
		return super.createContents(parent);
	}

	private void createSelectionGroup(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite radioGroup = new Composite(comp, SWT.NONE);
		radioGroup.setLayout(new GridLayout());
		radioGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		useWorkspaceSettingsButton = createRadioButton(radioGroup, "Use workspace settings");
		useProjectSettingsButton = createRadioButton(radioGroup, "Use project settings");
		
		configureButton = new Button(comp, SWT.PUSH);
		configureButton.setText("Configure workspace settings...");
		configureButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				configureWorkspaceSettings();
			}
		});
		
		// Set radio buttons
		try {
			IResource resource = (IResource) getElement();
			String use = resource.getPersistentProperty(new QualifiedName(pageId, USEPROJECTSETTINGS));
			if (TRUE.equals(use)) {
				useProjectSettingsButton.setSelection(true);
				configureButton.setEnabled(false);
			} else
				useWorkspaceSettingsButton.setSelection(true);
		} catch (CoreException e) {
			useWorkspaceSettingsButton.setSelection(true);
		}
	}

	private Button createRadioButton(Composite parent, String label) {
		final Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				configureButton.setEnabled(button == useWorkspaceSettingsButton);
				updateFieldEditors();
			}
		});
		return button;
	}

	@Override
	public IPreferenceStore getPreferenceStore() {
		if (isPropertyPage()) return overlayStore;
		return super.getPreferenceStore();
	}

	private void updateFieldEditors() {
		boolean enabled = useProjectSettingsButton.getSelection();
		updateFieldEditors(enabled);
	}

	protected void updateFieldEditors(boolean enabled) {
		Composite parent = getFieldEditorParent();		
		for (FieldEditor editor : fieldEditors) {
			editor.setEnabled(enabled, parent);
		}
	}
	
	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		if (result && isPropertyPage()) {
			// Save state of radiobuttons in project properties
			IResource resource = (IResource) getElement();
			try {
				String value = (useProjectSettingsButton.getSelection()) ? TRUE : FALSE;
				resource.setPersistentProperty(new QualifiedName(pageId, USEPROJECTSETTINGS), value);
			} catch (CoreException e) { }
		}
		return result;
	}

	@Override
	protected void performDefaults() {
		if (isPropertyPage()) {
			useWorkspaceSettingsButton.setSelection(true);
			useProjectSettingsButton.setSelection(false);
			configureButton.setEnabled(true);
			updateFieldEditors();
		}
		super.performDefaults();
	}

	protected void configureWorkspaceSettings() {
		try {
			IPreferencePage page = (IPreferencePage) this.getClass().newInstance();
			page.setTitle(getTitle());
			page.setImageDescriptor(image);
			showPreferencePage(pageId, page);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	protected void showPreferencePage(String id, IPreferencePage page) {
		final IPreferenceNode targetNode = new PreferenceNode(id, page);
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog = new PreferenceDialog(getControl().getShell(), manager);
		
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			@Override
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				dialog.open();
			}
		});
	}

}
