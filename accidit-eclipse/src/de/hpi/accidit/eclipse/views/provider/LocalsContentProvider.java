package de.hpi.accidit.eclipse.views.provider;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import org.cthul.miro.MiFuture;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.Pending;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class LocalsContentProvider implements ILazyTreeContentProvider {
		
	private final TreeViewer viewer;
	private int testId;
	private long callStep;
	private long step;
	private NamedValue[] root;
	private final DoInUiThread<NamedValue> updateNamedValue = new DoInUiThread<NamedValue>() {
		@Override
		public Void call(MiFuture<NamedValue> param) throws Exception {
			return super.call(param);
		}
		@Override
		protected void run(NamedValue value, Throwable error) {
			if (error != null) {
				error.printStackTrace(System.err);
			}
			viewer.setChildCount(value, 0);
			viewer.setChildCount(value, value.getValue().getChildren().length);
			viewer.update(value, null);
		}
	};
	
	public LocalsContentProvider(TreeViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public void dispose() { }

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { }
	
	public void setStep(int testId, long call, long step) {
		this.testId = testId;
		this.callStep = call;
		this.step = step;
		root = cnn().select()
				.from(NamedValue.VARIABLE_VIEW)
				.where().atStep(testId, callStep, step)
				.asArray()._execute();
		viewer.setChildCount(this, 0);
		viewer.setChildCount(this, root.length);
		viewer.update(this, null);
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public void updateChildCount(Object element, int currentCount) {
		if (element == this) {
			viewer.setChildCount(element, root == null ? 0 : root.length);
			return;
		}
		NamedValue v = (NamedValue) element;
		if (!v.isInitialized()) {
			updateLazy(v);
			return;
		}
		if (!v.getValue().hasChildren()) {
			viewer.setChildCount(v, 0);
			return;
		}
		viewer.setChildCount(v, v.getValue().getChildren().length);
	}

	@Override
	public void updateElement(Object parent, int index) {
		if (parent == this) {
			viewer.replace(parent, index, root[index]);
			updateChildCount(root[index], -1);
			return;
		}
		NamedValue v = (NamedValue) parent;
		if (!v.isInitialized()) {
			updateLazy(v);
			return;
		}
		viewer.replace(v, index, v.getValue().getChildren()[index]);
		updateChildCount(v.getValue().getChildren()[index], -1);
	}

	private void updateLazy(NamedValue v) {
		viewer.setChildCount(v, 1);
		viewer.replace(v, 1, new Pending());
		v.onInitialized(updateNamedValue);
	}
	
}
