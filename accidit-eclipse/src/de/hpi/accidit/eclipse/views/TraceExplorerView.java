package de.hpi.accidit.eclipse.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.commands.Command;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.part.ViewPart;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.handlers.AbstractToggleHandler;
import de.hpi.accidit.eclipse.handlers.ToggleAutoCollapseHandler;
import de.hpi.accidit.eclipse.handlers.ToggleCustomArrowNavigationHandler;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.Pending;
import de.hpi.accidit.eclipse.model.Trace;
import de.hpi.accidit.eclipse.model.TraceElement;
import de.hpi.accidit.eclipse.slice.DynamicSlice;
import de.hpi.accidit.eclipse.slice.ValueKey;
import de.hpi.accidit.eclipse.slice.ValueKey.InvocationKey;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class TraceExplorerView extends ViewPart implements ISelectionChangedListener, AcciditView {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "de.hpi.accidit.eclipse.views.TraceExplorerView";
	
	private TraceNavigatorUI ui;
	private TreeViewerSelectionAdapter treeViewerSelectionAdapter;
	private TreeViewer treeViewer;

	private static final String STORE_PROJECT_NAME = "Accidit.ProjectName";
	private static final String STORE_TEST_ID = "Accidit.TestId";
	private IMemento memento;
	
	private TraceElement current;

	public TraceExplorerView() { }

	@Override
	public void createPartControl(Composite parent) {		
		// create view elements
		Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);
		treeViewer = new TreeViewer(tree);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.setUseHashlookup(true);
		
		TreeColumn column0 = new TreeColumn(treeViewer.getTree(), SWT.LEFT);
		column0.setText("Method");
		TreeColumn column1 = new TreeColumn(treeViewer.getTree(), SWT.RIGHT);
		column1.setText("Call Step");
		
		TreeColumnLayout layout = new TreeColumnLayout();
		parent.setLayout(layout);
		layout.setColumnData(column0, new ColumnWeightData(90));
		layout.setColumnData(column1, new ColumnWeightData(10, 50));
		
		treeViewer.setContentProvider(new TraceContentProvider(treeViewer));
		treeViewer.setLabelProvider(new TraceLabelProvider());
		getSite().setSelectionProvider(treeViewer);
		treeViewer.addSelectionChangedListener(this);
		
		ui = TraceNavigatorUI.getGlobal();
		ui.setTraceExplorer(this);
		
		treeViewer.getControl().addKeyListener(new TraceExplorerKeyAdapter());
		treeViewerSelectionAdapter = new TreeViewerSelectionAdapter();
		
		/* Context menu registration. */
		MenuManager menuManager = new MenuManager();
		menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		final Menu contextMenu = menuManager.createContextMenu(treeViewer.getTree());
		treeViewer.getControl().setMenu(contextMenu);		
		getSite().registerContextMenu(menuManager, treeViewer);
		
		// restore project name
		if (memento != null) {
			try {
				String projectName = memento.getString(STORE_PROJECT_NAME);
				if (projectName != null && DatabaseConnector.getSelectedProject() == null) {
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (project != null) {
						DatabaseConnector.setSelectedProject(project);
					}
				}
				Integer testId = memento.getInteger(STORE_TEST_ID);
				if (testId == null) testId = 0;
				TraceNavigatorUI.getGlobal().setTestId(testId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/* Methods to restore the view state after eclipse has been closed. */
	
	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		IProject prj = DatabaseConnector.getSelectedProject();
		String projectName = (prj != null) ? prj.getName() : null;		
		memento.putString(STORE_PROJECT_NAME, projectName);
		if (current != null) memento.putInteger(STORE_TEST_ID, current.getTestId());
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}
	
	@Override
	public void dispose() {
		TraceNavigatorUI.getGlobal().removeView(this);
		super.dispose();
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}
	
	public TreeViewer getTreeViewer() {
		return treeViewer;
	}
	
	@Override
	public void setStep(TraceElement te) {
		if (current == te) {
			return;
		}
		if (current == null || current.getTestId() != te.getTestId()) {
			if (te.getTestId() == 65) {
				System.out.println("!slicing!");
				long callStep = 3688;
				ValueKey key = new InvocationKey(65, callStep);
//				DynamicSlice slice = new DynamicSlice(key);
//				slice.processAll();
//				SLICE.clear();
//				for (ValueKey k: slice.getSlice().keySet()) {
//					SLICE.add(k.getStep());
//				}
				System.out.println("!done!");
			}
			
			treeViewer.setInput(new Trace(te.getTestId(), ui));
		} else {
			getSelectionAdapter().selectAtStep(te.getStep());
		}
	}
	
	public void refresh() {
		treeViewer.refresh();
	}
	
	public TraceElement[] getRootElements() {
		Trace trace = (Trace) treeViewer.getInput();
		return trace.getRootElements();
	}
	
	/** Returns a TreeViewerSelectionAdapter instance to manipulate the treeViewer selection. */
	public TreeViewerSelectionAdapter getSelectionAdapter() {
		return treeViewerSelectionAdapter;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof ITreeSelection) {
			Object obj = ((ITreeSelection) selection).getFirstElement();

			if (obj instanceof TraceElement) {
				TraceElement te = (TraceElement) obj;
				current = te;
				ui.setStep(te);
			}
			setFocus();
		}	
	}
	
	/** Class to manipulate selections of the TraceExplorer's treeViewer */ 
	public class TreeViewerSelectionAdapter {
		
		/** Selects the currently selected trace element's parent. */
		public void selectParentElement() {
			TreeItem[] currentSelection = treeViewer.getTree().getSelection();
			if (currentSelection.length == 0) { // no selection
				return;
			}
			
			TreeItem selectedItem = currentSelection[0];
			TreeItem parent = selectedItem.getParentItem();
			if (parent != null) { // root item isn't selected
				treeViewer.setSelection(new StructuredSelection(parent.getData()));
				collapseSubElements(parent);
			}
		}
		
		/** Selects the currently selected trace element's first child. */
		public void selectFirstChildElement() {
			TreeItem[] currentSelection = treeViewer.getTree().getSelection();
			if (currentSelection.length == 0) { // no selection 
				return;
			}
			
			TreeItem selectedItem = currentSelection[0];
			treeViewer.expandToLevel(selectedItem.getData(), 1); // async load elements
			if (selectedItem.getItemCount() > 0) {
				treeViewer.setSelection(new StructuredSelection(selectedItem.getItem(0).getData()));
			}
		}
		
		/** Selects the currently selected trace element's last child. */
		public void selectLastChildElement() {
			TreeItem[] currentSelection = treeViewer.getTree().getSelection();
			if (currentSelection.length == 0) { // no selection 
				return;
			}
			
			TreeItem selectedItem = currentSelection[0];
			treeViewer.expandToLevel(selectedItem.getData(), 1); // async load elements
			if (selectedItem.getItemCount() > 0) {
				TreeItem lastChild = selectedItem.getItem(selectedItem.getItemCount() - 1);
				treeViewer.setSelection(new StructuredSelection(lastChild.getData()));
			}
		}
		
		/** Selects the currently selected trace element's previous sibling. */
		public void selectPreviousElement() {
			TreeItem[] currentSelection = treeViewer.getTree().getSelection();
			if (currentSelection.length == 0) { // no selection
				return;
			}
			
			TreeItem selectedItem = currentSelection[0];
			TreeItem parent = selectedItem.getParentItem();
			if (parent != null) { // root item isn't selected
				int itemIndex = parent.indexOf(selectedItem);
				if (itemIndex > 0) {
					TreeItem previousSibling = parent.getItems()[itemIndex - 1];
					treeViewer.setSelection(new StructuredSelection(previousSibling.getData()));
				} else {
					treeViewer.setSelection(new StructuredSelection(parent.getData()));
					collapseSubElements(parent);
				}
			}
		}
		
		/** Selects the currently selected trace element's next sibling. */
		public void selectNextElement() {
			TreeItem[] currentSelection = treeViewer.getTree().getSelection();
			if (currentSelection.length == 0) { // no selection
				TreeItem[] rootItems = treeViewer.getTree().getItems();
				if (rootItems.length > 0) {
					treeViewer.setSelection(new StructuredSelection(rootItems[0].getData()));
				}
				return;
			}

			TreeItem selectedItem = currentSelection[0];
			TreeItem parent = selectedItem.getParentItem();
			if (parent != null) { // root item isn't selected
				int index = parent.indexOf(selectedItem);
				if (parent.getItemCount() > index + 1) { // selectedItem is not the last child of its parent
					TreeItem nextSibling = parent.getItems()[index + 1];
					treeViewer.setSelection(new StructuredSelection(nextSibling.getData()));
				} else {
					TreeItem parentsParent = parent.getParentItem();
					if (parentsParent == null) { // root element's last child currently selected
						treeViewer.setSelection(null);
						return;
					}
					
					int parentIndex = parentsParent.indexOf(parent);
					TreeItem nextParentSibling = parentsParent.getItem(parentIndex + 1); 
					treeViewer.setSelection(new StructuredSelection(nextParentSibling.getData()));
					collapseSubElements(parent);
				}
			}
		}
		
		/** Selects the calling trace element of certain trace element that is identified by its step. */
		public void selectAtStep(long step) {
			TraceElement[] elements = TraceExplorerView.this.getRootElements();
			List<Object> pathSegments = new ArrayList<Object>();
			
			while (true) {
				TraceElement currentElement = null;
				for (int i = 0; i < elements.length; i++) {
					currentElement = elements[i];
					
					if (currentElement.getStep() == step) {
						pathSegments.add(currentElement);
						treeViewer.setSelection(new TreeSelection(new TreePath(pathSegments.toArray())));
						return;
					}

					// Too far in the tree - go back to previous element. 
					if (currentElement.getStep() > step) {
						if (i >= 1) currentElement = elements[i - 1];
						break;
					}
				}
				
				if (currentElement == null) return;
				pathSegments.add(currentElement);
				
				if (currentElement instanceof Invocation) {
					treeViewer.expandToLevel(new TreePath(pathSegments.toArray()), 1);
					elements = ((Invocation) currentElement).getChildren();
				} else {
					treeViewer.setSelection(new TreeSelection(new TreePath(pathSegments.toArray()).getParentPath()));
					return;
				}			
			}
		}
		
		private void collapseSubElements(TreeItem element) {
			if (autoCollapseEnabled())
				treeViewer.collapseToLevel(element.getData(), 1);
		}
		
		private boolean autoCollapseEnabled() {
			ICommandService commandService = (ICommandService) PlatformUI
					.getWorkbench().getActiveWorkbenchWindow()
					.getService(ICommandService.class);
			Command command = commandService.getCommand(ToggleAutoCollapseHandler.ID);
			return AbstractToggleHandler.getCommandState(command);
		}
	}
	
	private class TraceExplorerKeyAdapter extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			if (!customArrowNavigationEnabled()) return;
			
			e.doit = false;
			TreeViewerSelectionAdapter selectionAdapter = TraceExplorerView.this.getSelectionAdapter();
			
			switch(e.keyCode) {
			case SWT.ARROW_UP: selectionAdapter.selectPreviousElement(); break;
			case SWT.ARROW_LEFT: selectionAdapter.selectParentElement(); break;
			case SWT.ARROW_RIGHT: selectionAdapter.selectFirstChildElement(); break;
			case SWT.ARROW_DOWN: selectionAdapter.selectNextElement(); break;
			default: break;
			}
		}
		
		private boolean customArrowNavigationEnabled() {
			ICommandService commandService = (ICommandService) PlatformUI
					.getWorkbench().getActiveWorkbenchWindow()
					.getService(ICommandService.class);
			Command command = commandService.getCommand(ToggleCustomArrowNavigationHandler.ID);
			return AbstractToggleHandler.getCommandState(command);
		}
	}
	
	public static class TraceContentProvider implements ILazyTreeContentProvider {
		
		private TreeViewer viewer;
		private Trace trace = null;
		
		public TraceContentProvider(TreeViewer viewer) {
			this.viewer = viewer;
		}
		
		private DoInUiThread<Invocation> updateInvocation = new DoInUiThread<Invocation>() {
			@Override
			protected void run(Invocation inv, Throwable error) {
				if (error != null) {
					error.printStackTrace(System.err);
				} else {
					viewer.setChildCount(inv, 0);
					viewer.setChildCount(inv, inv.getChildren().length);
					viewer.update(inv, null);
				}
			}
		};

		@Override
		public void dispose() { }

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			trace = (Trace) newInput;
		}

		@Override
		public Object getParent(Object arg0) {
			return null;
		}

		@Override
		public void updateChildCount(Object element, int currentChildCount) {
			if (element instanceof Trace) {
				viewer.setChildCount(element, trace.root.length);
			}
			if (element instanceof Invocation) {
				final Invocation inv = (Invocation) element;
				if (inv.isInitialized()) {
					viewer.setChildCount(inv, inv.getChildren().length);
				} else {
					viewer.setChildCount(inv, 1);
					viewer.replace(inv, 0, new Pending());
					inv.onInitialized( updateInvocation);
				}
			}
		}

		@Override
		public void updateElement(Object parent, final int index) {
			if (parent instanceof Trace) {
				Trace trace = (Trace) parent;
				viewer.replace(parent, index, trace.root[index]);
				updateChildCount(trace.root[index], -1);
			}
			if (parent instanceof Invocation) {
				final Invocation inv = (Invocation) parent;
				if (!inv.isInitialized()) {
					System.out.println(" --- should not happen --- ");
					inv.beInitialized();
				}
				viewer.replace(inv, index, inv.getChildren()[index]);
				updateChildCount(inv.getChildren()[index], -1);
			}
		}
	}
	
	public static class TraceLabelProvider extends LabelProvider implements	ITableLabelProvider, ITableColorProvider {

		private static final Map<String, Image> images = new HashMap<>();

		private static void addImage(String s) {
			Display d = Display.getDefault();
			Image img = new Image(d, TraceLabelProvider.class.getResourceAsStream("/" + s));
			images.put(s, img);
		}

		static {
			addImage("trace_line.png");
			addImage("trace_over.png");
			addImage("trace_over_fail.png");
			addImage("trace_return.png");
			addImage("trace_fail.png");
			addImage("trace_catch.png");
			addImage("trace_throw.png");
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0 && element instanceof TraceElement) {
				return images.get(((TraceElement) element).getImage());
			}
			return null;
		}
		
		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof Pending) {
				if (columnIndex == 0) return "Pending...";
				return "";
			}

			if (element instanceof String) {
				if (columnIndex == 0) return element.toString();
				return "";
			}

			if (columnIndex == 1 && element instanceof TraceElement) {
				return String.valueOf(((TraceElement) element).getStep());
			}

			if (element instanceof TraceElement) {
				TraceElement le = (TraceElement) element;
				switch(columnIndex) {
				case 0: return le.getShortText();
				case 1: return String.valueOf(le.getStep());
				default: return "";
				}
			}

			if(!(element instanceof Invocation)) {
				return "";
			}			

			Invocation method = (Invocation) element;

			switch(columnIndex) {
			case 0: return String.format("%s.%s", method.type, method.method);
			case 2: return getFileName(method);
			case 3: return String.valueOf(0);
			default: return null;
			}
		}

		/**
		 * Returns the name of the file the method is called in: example.java:lineNumber
		 * 
		 * @return the file name and the line number
		 */
		private String getFileName(Invocation method) {
			if (method.line < 1) return "";

			//String typeName = method.type.substring(method.type.lastIndexOf(".") + 1);
			return String.format("%s.java:%d", method.parent.type, method.line);
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (!(element instanceof TraceElement)) return null;
			TraceElement te = (TraceElement) element;
			
			SortedSet<Long> slice = TraceNavigatorUI.getGlobal().getSliceSteps();
			if (slice == null || slice.isEmpty()) return null;
			if (slice.contains(te.getStep())) return null;
			if (element instanceof Invocation) {
				Invocation inv = (Invocation) element;
				if (!slice.subSet(inv.getStep(), inv.exitStep+1).isEmpty()) {
					return null; // black
					//return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
			}
			return Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}		
	}
}
