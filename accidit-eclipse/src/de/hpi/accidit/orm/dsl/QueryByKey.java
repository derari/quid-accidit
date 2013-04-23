package de.hpi.accidit.orm.dsl;

public interface QueryByKey<Result> {

	QueryBuilder<Result> byKeys(Object... ids);
	
}
