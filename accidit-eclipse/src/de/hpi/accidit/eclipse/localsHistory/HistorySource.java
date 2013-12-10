package de.hpi.accidit.eclipse.localsHistory;

import org.cthul.miro.MiConnection;
import org.cthul.miro.MiFuture;
import org.cthul.miro.MiFutureAction;
import org.cthul.miro.util.FinalFuture;

import de.hpi.accidit.eclipse.localsHistory.LocalsHistoryContainer.HistoryNode;
import de.hpi.accidit.eclipse.model.Value;

public abstract class HistorySource {

	public abstract void show(HistoryNode content, long id);

	public abstract MiFuture<String> getTitle(MiConnection cnn, long step);

	/* Implementations */
	
	public static class MethodCallSource extends HistorySource {

		private final long testId;
		private final long callStep;

		public MethodCallSource(long testId, long callStep) {
			this.testId = testId;
			this.callStep = callStep;
		}

		@Override
		public void show(HistoryNode content, long id) {
			content.showVariables(testId, callStep, id);
		}

		@Override
		public MiFuture<String> getTitle(MiConnection cnn, long step) {
			String s = String.valueOf("#" + callStep);
			return new FinalFuture<String>(s);
		}
	}

	public static class ObjectSource extends HistorySource {

		private final long testId;
		private final long thisId;
		private final boolean isArray;

		public ObjectSource(long testId, long thisId, boolean isArray) {
			this.testId = testId;
			this.thisId = thisId;
			this.isArray = isArray;
		}

		@Override
		public void show(HistoryNode content, long id) {
			content.showFields(testId, thisId, id, isArray);
		}

		@Override
		public MiFuture<String> getTitle(MiConnection cnn, long step) {
			return cnn.select().from(Value.object((int) testId, thisId, step))
					.getSingle()._submit()
					.onComplete(new MiFutureAction<MiFuture<Value>, String>() {
						@Override
						public String call(MiFuture<Value> arg) throws Exception {
							if (arg.hasFailed()) {
								arg.getException().printStackTrace(System.err);
								return arg.getException().getMessage();
							}
							return arg.getResult().getLongString();
						}
					});
		}
	}
}
