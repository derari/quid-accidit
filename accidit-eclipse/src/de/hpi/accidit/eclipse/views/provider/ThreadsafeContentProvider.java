package de.hpi.accidit.eclipse.views.provider;

import java.util.ArrayList;
import java.util.List;

import org.cthul.miro.function.MiFunction;
import org.cthul.miro.futures.MiFuture;
import org.cthul.miro.futures.MiFutures;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import de.hpi.accidit.eclipse.model.ModelBase;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.util.DoInUiThread;
import de.hpi.accidit.eclipse.views.util.WorkPool;

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
		
		protected Object getValue() {
			return value;
		}
		
		public Object getNodeValue() {
			if (valueInitRequired) {
				initializeValue();
			}
			return getValue();
		}
		
		protected final synchronized void setValue(Object value) {
			Object old = this.value;
			this.value = value;
			if (old != value) {
				valueChanged(value);
				reinitializeValue();
			}
		}
		
		protected final synchronized void setValueSoftUpdate(Object value) {
			Object old = this.value;
			this.value = value;
			if (old != value) {
				valueChanged(value);
				initializeValue();
			}
		}
		
		protected void valueChanged(Object value) { }
		
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
		
//		private void updateChildCount() {
//			updateChildCount(lastSize);
//		}
		
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
			c.updateViewer();
		}
		
		// State Automaton
		
		/** Makes the node displayable */
		private void makeActive() {
			if (nodeIsActive) return;
			nodeIsActive = true;
			nodeUpdateRequired = true;
			ensureValueInitialized();
			ensureNodeUpdated();
		}
		
		private void ensureValueInitialized() {
			if (!valueInitRequired) return;
			initializeValue();
		}
		
		protected synchronized void reinitializeValue() {
			nodeUpdateRequired = true;
			initializeValue();
		}
		
		protected void initializeValue() {
//			if (!nodeIsActive) {
//				valueInitRequired = true;
//				valueIsInitialized = false;
//				return;
//			}
			triggerValueInitialize();
			runNodeUpdate();
		}
		
		/**
		 * Lets the value initialize, then updates this node.
		 */
		private void triggerValueInitialize() {
			valueInitRequired = false;
			valueIsInitialized = false;
			final Object value = getValue();
			if (value instanceof ModelBase) {
				((ModelBase) value).onInitComplete(onValueInitialized());
			} else {
				WorkPool.execute(() -> {
					try {
						initializeValueAsynch(value);
						onValueInitialized().call(MiFutures.value(value));
					} catch (Throwable e) {
						e.printStackTrace();
					}
				});
			}
		}
		
		protected void initializeValueAsynch(Object value) throws Exception { }
		
		private synchronized void valueHasUpdated() {
			valueIsInitialized = true;
			nodeUpdateRequired = true;
			ensureViewUpdated();
		}
		
		private synchronized void ensureViewUpdated() {
			if (!nodeIsActive) return;
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
			if (!nodeIsActive) return;
			int count = getSize();
			if (lastSize != count) {
				viewer.setChildCount(this, count);
//				int i = lastSize;
				lastSize = count;
//				for (i = i < 0 ? 0 : i; i < count; i++) {
//					viewer.replace(this, i, getChild(i));
//				}
			}
			viewer.update(this, null);
		}
		
		protected boolean preAsyncUpdate(Object value) {
			return value == getValue();
		}

		
		protected void runNodeUpdate() {
			if (!nodeIsActive) return;
//			nodeUpdateRequired = true;
			ensureViewUpdated();
//			final Object value = getValue();
//			Display.getDefault().asyncExec(new Runnable() {
//				@Override
//				public void run() {
//					if (preAsyncUpdate(value)) {
//						valueHasChanged();
//					}
//				}
//			});			
		}
		
		public MiFunction<MiFuture<?>, ?> onValueInitialized() {
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
		
		public void setNamedValue(NamedValue nv) {
			setValue(nv);
		}
		
		@Override
		protected void valueChanged(Object value) {
			nv = (NamedValue) value;
			if (nv != null && !lastName.equals(nv.getName())) {
				viewer.setExpandedState(this, false);
				lastName = nv.getName();
				if (lastName == null) lastName = "";
			}
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
			if (nv == null) {
				if (getValue() == null) return;
				System.out.println("!!!!");
				nv = (NamedValue) getValue();
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
