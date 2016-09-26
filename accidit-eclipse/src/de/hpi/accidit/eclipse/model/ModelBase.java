package de.hpi.accidit.eclipse.model;

import org.cthul.miro.function.MiConsumer;
import org.cthul.miro.function.MiFunction;
import org.cthul.miro.futures.MiFuture;
import org.cthul.miro.futures.MiFutures;
import org.cthul.miro.futures.MiResettableFuture;

import de.hpi.accidit.eclipse.model.db.TraceDB;

public class ModelBase {
	
	private final MiResettableFuture<ModelBase> fInit = MiFutures
			.action(A_INIT.curry(this)).getTrigger();
	
	private TraceDB db = null;
	
	public ModelBase() {
	}
	
	public ModelBase(TraceDB db) {
		this.db = db;
	}

	public TraceDB db() {
		return db;
	}
	
	public boolean isInitialized() {
		return fInit.isDone();
	}
	
	public boolean beInitialized() {
		return fInit.beDone();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> MiFuture<T> onInitialized(MiFunction<? extends ModelBase, T> action) {
		return fInit.andThen((MiFunction) action);
	}
	
	public <T> MiFuture<T> onInitComplete(MiFunction<? extends MiFuture<?>, T> action) {
		return fInit.andDo((MiFunction) action);
	}
	
	protected boolean isInitSuccess() {
		return fInit.hasResult();
	}
	
	protected Throwable getInitException() {
		return fInit.getException();
	}
	
	protected void reInitialize() {
		fInit.reset();
	}

	protected void lazyInitialize() throws Exception {
	}
	
	private static final MiConsumer<ModelBase> A_INIT = ModelBase::lazyInitialize;
}
