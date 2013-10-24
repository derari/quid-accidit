package de.hpi.accidit.eclipse.views.provider;

import java.util.ArrayList;
import java.util.List;

import org.cthul.miro.MiFuture;
import org.cthul.miro.MiFutureAction;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;

public class ThreadsafeContentProvider implements ILazyTreeContentProvider {
	
	public static final ThreadsafeContentProvider INSTANCE = new ThreadsafeContentProvider();
		
	public ThreadsafeContentProvider() {
	}

	@Override
	public void dispose() { }

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { 
	}
	
	@Override
	public Object getParent(Object element) {
		return ((ContentNode) element).parent;
	}

	@Override
	public void updateChildCount(Object element, int currentCount) {
		// should call viewer#setChildCount
		((ContentNode) element).updateChildCount(currentCount);
	}

	@Override
	public void updateElement(Object parent, int index) {
		// should call viewer#replace and viewer#setChildCount
		((ContentNode) parent).updateChild(index);
	}
	
	public static class ContentNode {
		
		protected final TreeViewer viewer;
		protected final ContentNode parent;
		private final int depth;
		private final List<ContentNode> children = new ArrayList<>();
		private Object value;
		private int size = 0;
		private int lastSize = -1;
		private boolean initRequired = false;
		private boolean isActive = false;
		private boolean isInvalid = true;
		
		private final DoInUiThread<Object> asyncUpdate = new DoInUiThread<Object>() {
			@Override
			protected void run(Object value, Throwable error) {
				if (error != null) {
					error.printStackTrace(System.err);
				} else {
					if (preAsyncUpdate(value)) {
						updateSelf();
					}
				}
			}
		};
		
		public ContentNode(TreeViewer viewer) {
			this.viewer = viewer;
			this.parent = null;
			this.depth = 0;
		}
		
		public ContentNode(ContentNode parent) {
			this.parent = parent;
			this.viewer = parent.viewer;
			this.depth = parent.depth + 1;
		}
		
		public boolean isInvalid() {
			return isInvalid;
		}
		
		public Object getValue() {
			return value;
		}
		
		public void setValue(Object value) {
			Object old = this.value;
			this.value = value;
			if (old != value || isInvalid) {
				initRequired = true;
				initIfActive();
			}
		}
		
		public ContentNode getChild(int i) {
			while (children.size() <= i) {
				children.add(newNode());
			}
			return children.get(i);
		}
		
		protected ContentNode newNode() {
			return new ContentNode(this);
		}
		
		public int getDepth() {
			return depth;
		}
		
		public int getSize() {
			return size;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		protected void invalidate() {
			isInvalid = true;
			updateSelf();
			int s = getSize();
			for (int i = 0; i < s; i++) {
				getChild(i).invalidate();
			}
		}
		
		protected void makeValid() {
			isInvalid = false;
		}
		
		protected synchronized void makeInitialized() {
			isActive = true;
			if (initRequired) {
				initRequired = false;
				initialize();
			}
		}
		
		protected void initialize() {
		}
		
		protected boolean preAsyncUpdate(Object value) {
			return value == getValue();
		}
		
		public void makeActive() {
			// implicit call to makeInitialized();
			updateChildCount();
		}
		
		public void updateChildCount() {
			updateChildCount(lastSize);
		}
		
		public void updateChildCount(int lastCount) {
			makeInitialized();
			int count = getSize();
			if (lastCount != count) {
//				int i = lastCount < 0 ? 0 : lastCount;
				lastSize = count;
				viewer.setChildCount(this, count);
				
//				for (; i < count; i++) {
//					update(i);
//				}
			}
		}
		
		public void updateChild(int i) {
			makeInitialized();
			ContentNode c = getChild(i);
			viewer.replace(this, i, c);
			c.makeActive();
		}
		
		public void initIfActive() {
			if (isActive) {
				makeInitialized();
			}
		}
		
		protected void updateSelf() {
			isActive = true;
			makeActive();
			viewer.update(this, null); 
//			int s = getSize();
//			for (int i = 0; i < s; i++) {
//				getChild(i).updateIfActive();
//			}
		}
		
		protected void runUpdate() {
			final Object value = getValue();
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (preAsyncUpdate(value)) {
						updateSelf();
					}
				}
			});			
		}
		
		public MiFutureAction<MiFuture<?>, ?> asyncUpdate() {
			return asyncUpdate;
		}
		
	}
	
	public static class NamedValueNode extends ContentNode {

		private String lastName = "";
		protected NamedValue nv = null;
		
		public NamedValueNode(TreeViewer viewer) {
			super(viewer);
		}
		
		public NamedValueNode(ContentNode parent) {
			super(parent);
		}
		
		public void updateStep(long newStep) {
			if (nv.setStep(newStep)) {
				int s = getSize();
				for (int i = 0; i < s; i++) {
					((NamedValueNode) getChild(i)).updateStep(newStep);
				}
			} else {
				invalidate();
				nv.onInitialized(asyncUpdate());
			}
		}
		
		@Override
		public void setValue(Object value) {
			nv = (NamedValue) value;
			if (nv != null && !lastName.equals(nv.getName())) {
				viewer.setExpandedState(this, false);
				lastName = nv.getName();
				if (lastName == null) lastName = "";
			}
			super.setValue(value);
		}
		
		@Override
		protected synchronized void initialize() {
			if (!nv.isInitialized()) {
				setSize(0);
			}
			nv.onInitialized(asyncUpdate());
		}
		
		@Override
		protected boolean preAsyncUpdate(Object value) {
			if (value != nv) {
//				System.out.println("Skipped " + value);
				return false;
			}
//			System.out.println("Initialized " + nv + " " + nv.getValue().getChildren().length);
			if (!nv.isInitialized()) {
				return false;
			}
			makeValid();
			NamedValue[] children = nv.getValue().getChildren();
			setSize(children.length);
			for (int i = 0; i < children.length; i++) {
				getChild(i).setValue(children[i]);
			}
			return super.preAsyncUpdate(value);
		}
		
		@Override
		protected ContentNode newNode() {
			return new NamedValueNode(this);
		}
	}
	
}
