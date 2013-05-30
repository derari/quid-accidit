package de.hpi.accidit.eclipse;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import de.hpi.accidit.eclipse.views.LocalsExplorerView;
import de.hpi.accidit.eclipse.views.MethodExplorerView;
import de.hpi.accidit.eclipse.views.NavigatorView;

public class AcciditPerspectiveFactory implements IPerspectiveFactory {
	
	private static final String METHOD_EXPLORER_VIEW_ID = MethodExplorerView.ID;
	private static final String LOCALS_EXPLORER_VIEW_ID = LocalsExplorerView.ID;
	private static final String NAVIGATOR_VIEW_ID = NavigatorView.ID;
	
	private static final String LEFT_FOLDER = "left";

	@Override
	public void createInitialLayout(IPageLayout layout) {
		IFolderLayout left = layout.createFolder(LEFT_FOLDER, IPageLayout.LEFT, 0.20f, layout.getEditorArea());
		left.addView(IPageLayout.ID_PROJECT_EXPLORER);
		left.addView(IPageLayout.ID_OUTLINE);

		layout.addView(METHOD_EXPLORER_VIEW_ID, IPageLayout.BOTTOM, 0.70f, layout.getEditorArea());
		layout.addView(LOCALS_EXPLORER_VIEW_ID, IPageLayout.RIGHT, 0.70f, layout.getEditorArea());
		
		layout.addShowViewShortcut(METHOD_EXPLORER_VIEW_ID);
		layout.addShowViewShortcut(LOCALS_EXPLORER_VIEW_ID);
		layout.addShowViewShortcut(NAVIGATOR_VIEW_ID);
	}

}
