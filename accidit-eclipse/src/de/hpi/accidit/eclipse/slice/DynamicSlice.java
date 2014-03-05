package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.List;

public class DynamicSlice {

	public DynamicSlice() {
	}
	
	public static class Node {
		private final List<Node> dependencies = new ArrayList<>();
	}
	
	public static class ValueKey implements Comparable<ValueKey> {
		protected final long step;
		public ValueKey(long step) {
			this.step = step;
		}
		@Override
		public int compareTo(ValueKey o) {
			long cStep = (step - o.step);
			int c = cStep < 0 ? -1 : (cStep > 0 ? 1 : 0);
			if (c != 0) return c;
			c = getClass().getName().compareTo(o.getClass().getName());
			if (c != 0) return c;
			return specificCompareTo(o);
		}
		protected int specificCompareTo(ValueKey o) {
			return 0;
		}
	}

}
