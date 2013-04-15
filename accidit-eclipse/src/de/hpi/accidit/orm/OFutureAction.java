package de.hpi.accidit.orm;

public interface OFutureAction<Param, Result> {

	Result call(Param param) throws Exception;
	
}
