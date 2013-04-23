package de.hpi.accidit.orm.dsl;

import de.hpi.accidit.orm.OConnection;

public interface View<Qry> {

	Qry newQuery(OConnection cnn, String[] select);
	
}
