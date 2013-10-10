package de.hpi.accidit.eclipse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;

public class BreakPointsView extends ViewPart {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.views.BreakPointsView";
	
	private List<Widget> headlineElements;
	
	public BreakPointsView() {
		headlineElements = new ArrayList<Widget>();
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(5, false);
		parent.setLayout(layout);

		addHeadline(parent);
		addBreakPointLine(parent);
		addBreakPointLine(parent);
	}

	@Override
	public void setFocus() { }
	
	private void addHeadline(final Composite parent) {
		final Label placeHolder1 = new Label(parent, SWT.NONE);
		headlineElements.add(placeHolder1);
		
		final Label typeLabel = new Label(parent, SWT.NONE);
		typeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		typeLabel.setText("Type");
		headlineElements.add(typeLabel);
		
		final Label locationLabel = new Label(parent, SWT.NONE);
		locationLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		locationLabel.setText("Location");
		headlineElements.add(locationLabel);
		
		final Label detailsLabel = new Label(parent, SWT.NONE);
		detailsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		detailsLabel.setText("Location details");
		headlineElements.add(detailsLabel);

//		final Label placeHolder2 = new Label(parent, SWT.NONE);
//		headlineElements.add(placeHolder2);
		
		final Button removeButton = new Button(parent, SWT.BORDER);
		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		removeButton.setText("X");
		removeButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				placeHolder1.dispose();
				typeLabel.dispose();
				locationLabel.dispose();
				detailsLabel.dispose();
				removeButton.dispose();
				parent.layout();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { }
		});
	}
	
	public void addBreakPointLine(final Composite parent) {
		final Button detailsButton = new Button(parent, SWT.BORDER);
		detailsButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		detailsButton.setText("v");
		
		final Combo typeCombo = new Combo(parent, SWT.READ_ONLY | SWT.V_SCROLL);
		typeCombo.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		typeCombo.setItems(new String[] {"line", "field", "exception"});
		typeCombo.setText("line");
		
		final Text locationText = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		locationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final Text detailsText = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL);
		detailsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final Button removeButton = new Button(parent, SWT.BORDER);
		removeButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		removeButton.setText("X");
		removeButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				detailsButton.dispose();
				typeCombo.dispose();
				locationText.dispose();
				detailsText.dispose();
				removeButton.dispose();
				parent.layout();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) { }
		});
	}

}
