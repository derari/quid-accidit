package de.hpi.accidit.eclipse.model;

import org.cthul.miro.MiFuture;
import org.cthul.miro.MiFutureAction;
import org.cthul.miro.util.LazyFuture;

import de.hpi.accidit.eclipse.DatabaseConnector;

public class ModelBase {
	
	private final MiFuture<ModelBase> fInit = new LazyFuture<ModelBase>() {
		@Override
		protected MiFuture<ModelBase> initialize() throws Exception {
			return DatabaseConnector.cnn().submit(A_INIT, ModelBase.this);
		}
	};
	
	public boolean isInitialized() {
		return fInit.isDone();
	}
	
	public boolean beInitialized() {
		return fInit.beDone();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> MiFuture<T> onInitialized(MiFutureAction<? extends MiFuture<? extends ModelBase>, T> action) {
		return fInit.onComplete((MiFutureAction) action); 
	}
	
	protected boolean isInitSuccess() {
		return fInit.hasResult();
	}
	
	protected Throwable getInitException() {
		return fInit.getException();
	}

	protected void lazyInitialize() throws Exception {
	}
	
	private static final MiFutureAction<ModelBase, ModelBase> A_INIT = new MiFutureAction<ModelBase, ModelBase>() {
		@Override
		public ModelBase call(ModelBase arg) throws Exception {
			arg.lazyInitialize();
			return arg;
		}
	};
	
}
