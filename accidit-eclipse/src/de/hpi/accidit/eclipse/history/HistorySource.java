package de.hpi.accidit.eclipse.history;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.futures.MiFuture;
import org.cthul.miro.futures.MiFutures;

import de.hpi.accidit.eclipse.history.HistoryContainer.HistoryNode;
import de.hpi.accidit.eclipse.model.Value;
import de.hpi.accidit.eclipse.model.db.TraceDB;

public abstract class HistorySource {

	public abstract void show(HistoryNode content, long id);

	public abstract MiFuture<String> getTitle(TraceDB db, long step);

	/* Implementations */
	
	public static class MethodCallSource extends HistorySource {

		private final int testId;
		private final long callStep;

		public MethodCallSource(int testId, long callStep) {
			this.testId = testId;
			this.callStep = callStep;
		}

		@Override
		public void show(HistoryNode content, long id) {
			content.showVariables(testId, callStep, (int) id);
		}

		@Override
		public MiFuture<String> getTitle(TraceDB db, long step) {
			String s = String.valueOf("#" + callStep);
			return MiFutures.value(s);
		}
	}

	public static class ObjectSource extends HistorySource {

		private final int testId;
		private final long thisId;
		private final boolean isArray;

		public ObjectSource(int testId, long thisId, boolean isArray) {
			this.testId = testId;
			this.thisId = thisId;
			this.isArray = isArray;
		}

		@Override
		public void show(HistoryNode content, long id) {
			content.showFields(testId, thisId, (int) id, isArray);
		}

		@Override
		public MiFuture<String> getTitle(TraceDB db, long step) {
			return db.values().ofObject((int) testId, thisId, step)
					.result().andDo(f -> {
						if (f.hasFailed()) {
							f.getException().printStackTrace(System.err);
							return f.getException().getMessage();
						}
						return f.getResult().getSingle().getLongString();
					});
		}
	}
}
