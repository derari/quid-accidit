package de.hpi.accidit.orm.dsl;

import de.hpi.accidit.orm.OConnection;

public class Select {
	
	private static final Select SELECT_ALL = new Select();
	
	public static Select select() {
		return SELECT_ALL;
	}
	
	public static Select select(String... fields) {
		return new Select(fields);
	}
	
	private final OConnection cnn;
	private final String[] fields;
	
	public Select() {
		this(null, (String[]) null);
	}
	
	public Select(OConnection cnn) {
		this(cnn, (String[]) null);
	}
	
	public Select(String... fields) {
		this(null, fields);
	}
	
	public Select(OConnection cnn, String... fields) {
		this.cnn = cnn;
		this.fields = fields;
	}

	public <Qry> Qry from(View<Qry> view) {
		return view.newQuery(cnn, fields);
	}

}
