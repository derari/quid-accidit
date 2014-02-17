package de.hpi.accidit.eclipse.views.provider;

import java.util.ArrayList;
import java.util.List;

import org.cthul.miro.MiFuture;
import org.cthul.miro.MiFutureAction;
import org.cthul.miro.util.FinalFuture;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import de.hpi.accidit.eclipse.model.ModelBase;
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
		
		/* 1) ContentProvider#update*
		 *      -update* 
		 *        -makeActive
		 *           -ensureValueInitialized
		 *             +reinitializeValue
		 *           -ensureNodeUpdated
		 *      viewer.update*
		 *      
		 * 2) onValueInitialized
		 *      +valueHasUpdated
		 * 
		 * 3) valueHasUpdated
		 *      -syncUpdateNode
		 *      -updateViewer
		 *      
		 * 4) reinitializeValue
		 *     -triggerValueInit
		 *       ->onValueInitialized
		 *     -ensureNodeUpdated
		 *     
		 * 5) setValue
		 *     -reinitializeValue
		 * 
		 */
		
		protected final TreeViewer viewer;
		protected final ContentNode parent;
		private final int depth;
		private final List<ContentNode> children = new ArrayList<>();
		private Object value;
		private int size = 0;
		private int lastSize = -1;
		
		private boolean nodeIsActive = false;
		private boolean nodeUpdateRequired = false;
		private boolean valueInitRequired = true;
		private boolean valueIsInitialized = false;
		
		private final DoInUiThread<Object> onValueInitialized = new DoInUiThread<Object>() {
			@Override
			protected void run(Object value, Throwable error) {
				if (error != null) {
					error.printStackTrace(System.err);
				} else {
					if (preAsyncUpdate(value)) {
						valueHasUpdated();
					}
				}
			}
		};
		
		public ContentNode(TreeViewer viewer) {
			this.viewer = viewer;
			this.parent = null;
			this.depth = 0;
			if (viewer.getContentProvider() == null) {
				viewer.setContentProvider(INSTANCE);
			}
		}
		
		public ContentNode(ContentNode parent) {
			this.parent = parent;
			this.viewer = parent.viewer;
			this.depth = parent.depth + 1;
		}
		
		public Object getValue() {
			return value;
		}
		
		protected synchronized void setValue(Object value) {
			Object old = this.value;
			this.value = value;
			if (old != value) {
				reinitializeValue();
				runUpdate();
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
		
		protected void setSize(int size) {
			this.size = size;
		}
		
		// via ContentProvider
		
		private void updateChildCount() {
			updateChildCount(lastSize);
		}
		
		synchronized void updateChildCount(int lastCount) {
			makeActive();
			int count = getSize();
			if (lastCount != count) {
				lastSize = count;
				viewer.setChildCount(this, count);
			}
		}
		
		synchronized void updateChild(int i) {
			makeActive();
			ContentNode c = getChild(i);
			viewer.replace(this, i, c);
			c.makeActive();
		}
		
		// State Automaton
		
		/** Makes the node displayable */
		private void makeActive() {
			nodeIsActive = true;
			ensureValueInitialized();
			ensureNodeUpdated();
		}
		
		private void ensureValueInitialized() {
			if (!valueInitRequired) return;
			reinitializeValue();
		}
		
		protected synchronized void reinitializeValue() {
			if (!nodeIsActive) {
				valueInitRequired = true;
				valueIsInitialized = false;
				return;
			}
			triggerValueInitialize();
			runUpdate();
		}
		
		/**
		 * Lets the value initialize, then updates this node.
		 */
		private void triggerValueInitialize() {
			valueInitRequired = false;
			valueIsInitialized = false;
			final Object value = getValue();
			if (value instanceof ModelBase) {
				((ModelBase) value).onInitialized(onValueInitialized());
			} else {
				new Thread() {
					public void run() {
						try {
							initializeValueAsynch(value);
							FinalFuture<Object> f = new FinalFuture<Object>(value);
							onValueInitialized().call(f);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					};
				}.start();
			}
		}
		
		protected void initializeValueAsynch(Object value) throws Exception { }
		
		private synchronized void valueHasUpdated() {
			valueIsInitialized = true;
			nodeUpdateRequired = true;
			ensureNodeUpdated();
			updateViewer();
		}
		
		private synchronized void valueHasChanged() {
			if (nodeUpdateRequired) {
				ensureNodeUpdated();
				updateViewer();
			}
		}
		
		private synchronized void ensureNodeUpdated() {
			if (!nodeUpdateRequired) return;
			if (!nodeIsActive) return;
			nodeUpdateRequired = false;
			updateNode(valueIsInitialized);
		}
		
		/** Initializer code of the node, synchronous */
		protected void updateNode(boolean valueIsInitialized) {
		}
		
		private void updateViewer() {
			int count = getSize();
			if (lastSize != count) {
				lastSize = count;
				viewer.setChildCount(this, count);
			}
			viewer.update(this, null);
		}
		
		protected boolean preAsyncUpdate(Object value) {
			return value == getValue();
		}

		
		protected void runUpdate() {
			nodeUpdateRequired = true;
			final Object value = getValue();
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (preAsyncUpdate(value)) {
						valueHasChanged();
					}
				}
			});			
		}
		
		public MiFutureAction<MiFuture<?>, ?> onValueInitialized() {
			return onValueInitialized;
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
				reinitializeValue();
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
		public NamedValue getValue() {
			return (NamedValue) super.getValue();
		}
		
		@Override
		protected void updateNode(boolean valueIsInitialized) {
			if (!valueIsInitialized) {
				setSize(0);
				return;
			}
			NamedValue[] children = nv.getValue().getChildren();
			setSize(children.length);
			for (int i = 0; i < children.length; i++) {
				getChild(i).setValue(children[i]);
			}
		}
		
		@Override
		protected ContentNode newNode() {
			return new NamedValueNode(this);
		}
	}
}
