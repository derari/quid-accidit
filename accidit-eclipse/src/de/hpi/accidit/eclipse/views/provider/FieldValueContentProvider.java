package de.hpi.accidit.eclipse.views.provider;

import org.cthul.miro.MiFuture;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import de.hpi.accidit.eclipse.model.Callback;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.model.Pending;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class FieldValueContentProvider implements ILazyTreeContentProvider {
		
	protected TreeViewer viewer = null;
	
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
//				if (value == root) viewer.refresh();
			}
		}
	};
	
	protected final Callback<NamedValue> cbUpdateNamedValue = new Callback<NamedValue>() {
		@Override
		public void call(NamedValue value) {
//			viewer.setChildCount(value, 0);
			System.out.println("call update " + value);
			viewer.update(value, null);
//			if (value == root) 
				viewer.refresh();
		}
	};
	
	@Override
	public void dispose() { }

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { 
		if (this.viewer == null) {
			this.viewer = (TreeViewer) viewer;
		} else if (this.viewer != viewer) {
			throw new IllegalArgumentException("viewer");
		}
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public void updateChildCount(Object element, int currentCount) {
		updateChildCount(element, (NamedValue) element, currentCount);
	}
	
	protected void updateChildCount(Object element, NamedValue v, int currentCount) {
		if (!v.isInitialized()) {
			updateLazy(v);
		}
		NamedValue[] c = v.previewChildren();
		if (c == null) {
			viewer.setChildCount(element, 0);
		} else {
			viewer.setChildCount(element, c.length);
		}
	}

	@Override
	public void updateElement(Object parent, int index) {
		updateElement(parent, (NamedValue) parent, index); 
	}
	
	protected void updateElement(Object parent, NamedValue v, int index) {
		if (!v.isInitialized()) {
			// only update `v` once
			if (index == 0) updateLazy(v);
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
	}

	private void updateLazy(NamedValue v) {
		System.out.println("lazy " + v);
		v.onInitialized(updateNamedValue);
	}
	
}
