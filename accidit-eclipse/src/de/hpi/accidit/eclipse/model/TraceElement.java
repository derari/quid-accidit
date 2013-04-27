package de.hpi.accidit.eclipse.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.dsl.QueryTemplate;
import de.hpi.accidit.orm.map.Mapping;
import de.hpi.accidit.orm.map.ResultBuilder;
import de.hpi.accidit.orm.map.ResultBuilder.ValueAdapter;

public class TraceElement implements Comparable<TraceElement> {

	public Invocation parent;
	public int line;
	public long step;
	
	@Override
	public int compareTo(TraceElement o) {
		int c = Long.compare(step, o.step);
		if (c != 0) return c;
		return deepCompare(o);
	}

	protected int deepCompare(TraceElement o) {
		return 0;
	}
	
	public String getImage() {
		return "";
	}
	
	public String getShortText() {
		return "";
	}

	
	protected static class TETemplate<V extends TraceElement> extends QueryTemplate<V> {{
		select("line", "f.line",
			   "step", "f.step");
		where("testId_EQ", "f.testId = ?");
		where("callStep_EQ", "f.callStep = ?");
		orderBy("o_step", "f.step");
	}};
	
	protected static class SetParentAdapter 
					implements ResultBuilder.ValueAdapter<TraceElement>, 
							   ResultBuilder.ValueAdapterFactory<TraceElement> {
		
		private final Invocation parent;
		
		public SetParentAdapter(Invocation parent) {
			this.parent = parent;
		}
		
		@Override
		public ValueAdapter<TraceElement> newAdapter(Mapping<TraceElement> mapping, OConnection cnn, List<String> attributes) {
			return this;
		}
		
		@Override
		public void initialize(ResultSet rs) throws SQLException {}
		
		@Override
		public void apply(TraceElement entity) throws SQLException {
			entity.parent = parent;
		}
		
		@Override
		public void complete() throws SQLException {}
		}
	
}
