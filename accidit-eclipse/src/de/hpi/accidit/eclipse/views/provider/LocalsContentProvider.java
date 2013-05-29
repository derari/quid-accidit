package de.hpi.accidit.eclipse.views.provider;

import static de.hpi.accidit.eclipse.DatabaseConnector.cnn;

import org.cthul.miro.MiFuture;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import de.hpi.accidit.eclipse.model.Callback;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.Pending;
import de.hpi.accidit.eclipse.model.Value;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class LocalsContentProvider implements ILazyTreeContentProvider {
		
	private final TreeViewer viewer;
	private int testId = -1;
	private long callStep = -1;
	private long step;
	private NamedValue root;
	private final DoInUiThread<NamedValue> updateNamedValue = new DoInUiThread<NamedValue>() {
		@Override
		public Void call(MiFuture<NamedValue> param) throws Exception {
			return super.call(param);
		}
		@Override
		protected void run(NamedValue value, Throwable error) {
			System.out.println("do in ui " + value);
			if (error != null) {
				error.printStackTrace(System.err);
			}
			if (value != null) {
//				viewer.setChildCount(value, 0);
//				viewer.setChildCount(value, value.getValue().getChildren().length);
//				updateChildCount(value, -1);
				
				viewer.update(value, null);
				updateChildCount(value, -1);
				if (value == root) viewer.refresh();
			}
		}
	};
	private final Callback<NamedValue> cbUpdateNamedValue = new Callback<NamedValue>() {
		@Override
		public void call(NamedValue value) {
//			viewer.setChildCount(value, 0);
			System.out.println("call update " + value);
			viewer.update(value, null);
//			if (value == root) 
				viewer.refresh();
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
		if (testId != this.testId || call != this.callStep) {
			root = new NamedValue.MethodFrameValue(testId, call, step);
			viewer.setInput(root);
		} else {
			root.updateValue(step, cbUpdateNamedValue);
			if (!root.isInitialized()) {
				viewer.update(root, null);
			}
		}
		
		this.testId = testId;
		this.callStep = call;
		this.step = step;
//		viewer.setInput(root);
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public void updateChildCount(Object element, int currentCount) {
		NamedValue v = (NamedValue) element;
		if (!v.isInitialized()) {
			updateLazy(v);
//			return;
		}
		NamedValue[] c = v.previewChildren();
		if (c == null) {
			viewer.setChildCount(v, 0);
		} else {
			viewer.setChildCount(v, c.length);
		}
//		if (!v.getValue().hasChildren()) {
//			viewer.setChildCount(v, 0);
//			return;
//		}
//		viewer.setChildCount(v, v.getValue().getChildren().length);
	}

	@Override
	public void updateElement(Object parent, int index) {
		NamedValue v = (NamedValue) parent;
		if (!v.isInitialized()) {
			if (index == 0) updateLazy(v);
//			return;
		}
		NamedValue[] c = v.previewChildren();
		NamedValue nv = c != null && c.length > index ? c[index] : null;
		if (nv == null) {
			Pending p = new Pending();
			viewer.replace(parent, index, p);
			viewer.setChildCount(p, 0);
		} else {
			System.out.println("set " + nv);
			viewer.replace(parent, index, nv);
			updateChildCount(nv, -1);
		}
//		viewer.replace(v, index, v.getValue().getChildren()[index]);
//		updateChildCount(v.getValue().getChildren()[index], -1);
	}

	private void updateLazy(NamedValue v) {
		System.out.println("lazy " + v);
		v.onInitialized(updateNamedValue);
	}
	
}
