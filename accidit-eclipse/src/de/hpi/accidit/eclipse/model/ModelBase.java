package de.hpi.accidit.eclipse.model;

import org.cthul.miro.MiConnection;
import org.cthul.miro.MiFuture;
import org.cthul.miro.MiFutureAction;
import org.cthul.miro.util.LazyAction;

import de.hpi.accidit.eclipse.DatabaseConnector;

public class ModelBase {
	
	private final LazyAction<ModelBase> fInit = new LazyAction<ModelBase>(DatabaseConnector.cnn(), this, A_INIT);
	private MiConnection cnn = null;
	
	public ModelBase() {
	}
	
	public ModelBase(MiConnection cnn) {
		this.cnn = cnn;
	}

	protected MiConnection cnn() {
		return cnn;
	}
	
	public boolean isInitialized() {
		return fInit.isDone();
	}
	
	public boolean beInitialized() {
		return fInit.beDoneNow();
	}
	
	public <T> MiFuture<T> onInitialized(MiFutureAction<?, T> action) {
		return fInit.onComplete((MiFutureAction) action); 
	}
	
	protected boolean isInitSuccess() {
		return fInit.hasResult();
	}
	
	protected Throwable getInitException() {
		return fInit.getException();
	}
	
	protected void reInitialize() {
		fInit.cancel(true);
		fInit.reset();
	}

	protected void lazyInitialize() throws Exception {
	}
	
	private static final MiFutureAction<ModelBase, ModelBase> A_INIT = new MiFutureAction<ModelBase, ModelBase>() {
		@Override
		public ModelBase call(ModelBase arg) throws Exception {
			try {
				arg.lazyInitialize();
				return arg;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				throw e;
			}
		}
	};
	
}
