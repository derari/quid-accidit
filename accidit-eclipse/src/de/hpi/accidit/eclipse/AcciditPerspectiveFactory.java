package de.hpi.accidit.eclipse;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import de.hpi.accidit.eclipse.breakpoints.BreakpointsView;
import de.hpi.accidit.eclipse.history.HistoryView;
import de.hpi.accidit.eclipse.slice.SlicingCriteriaView;
import de.hpi.accidit.eclipse.views.NavigatorView;
import de.hpi.accidit.eclipse.views.TraceExplorerView;
import de.hpi.accidit.eclipse.views.VariablesView;

public class AcciditPerspectiveFactory implements IPerspectiveFactory {
	
	private static final String LEFT_FOLDER = "left";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		IFolderLayout left = layout.createFolder(LEFT_FOLDER, IPageLayout.LEFT, 0.20f, layout.getEditorArea());
		left.addView(IPageLayout.ID_PROJECT_EXPLORER);
		left.addView(IPageLayout.ID_OUTLINE);

		layout.addView(TraceExplorerView.ID, IPageLayout.BOTTOM, 0.70f, layout.getEditorArea());
		layout.addView(VariablesView.ID, IPageLayout.RIGHT, 0.70f, layout.getEditorArea());
		
		layout.addShowViewShortcut(TraceExplorerView.ID);
		layout.addShowViewShortcut(VariablesView.ID);
		layout.addShowViewShortcut(HistoryView.ID);
		layout.addShowViewShortcut(NavigatorView.ID);
		layout.addShowViewShortcut(BreakpointsView.ID);
		layout.addShowViewShortcut(SlicingCriteriaView.ID);
	}
}
