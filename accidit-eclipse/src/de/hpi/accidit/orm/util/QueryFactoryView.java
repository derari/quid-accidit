package de.hpi.accidit.orm.util;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.dsl.View;


public class QueryFactoryView<Qry> implements View<Qry> {
	
	private final Constructor<Qry> newQuery;

	public QueryFactoryView(Class<Qry> clazz) {
		try {
			newQuery = clazz.getConstructor(OConnection.class, String[].class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Qry newQuery(OConnection cnn, String[] select) {
		try {
			return newQuery.newInstance(cnn, select);
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}

}
