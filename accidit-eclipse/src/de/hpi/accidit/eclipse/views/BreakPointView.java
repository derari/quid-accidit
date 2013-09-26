package de.hpi.accidit.eclipse.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class BreakPointView extends ViewPart {
	
	public static final String ID = "de.hpi.accidit.eclipse.views.BreakPointView";
	
	Composite  grpBreakCondition;

	@Override
	public void createPartControl(Composite parent) {
		
		Display display = parent.getDisplay();
		
		RowLayout rowLayout = new RowLayout();
		rowLayout.type = SWT.VERTICAL;
		rowLayout.fill = true;
		parent.setLayout(rowLayout);
		
		Combo ddBreakType = new Combo(parent, SWT.DROP_DOWN | SWT.BORDER);
		ddBreakType.add("Line");
		
		grpBreakCondition = new Composite(parent, SWT.NONE);
		grpBreakCondition.setBackground(display.getSystemColor(SWT.COLOR_RED));
		StackLayout sl = new StackLayout();
		sl.marginHeight = sl.marginWidth = 0;
		grpBreakCondition.setLayout(sl);
		
		Text txtLine = new Text(grpBreakCondition, SWT.BORDER);
		txtLine.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
		sl.topControl = txtLine;
		
	}
	
	private void selectLine() {
		
	}

	@Override
	public void setFocus() {
	}

}
