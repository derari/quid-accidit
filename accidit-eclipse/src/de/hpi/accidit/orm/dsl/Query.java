package de.hpi.accidit.orm.dsl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OPreparedStatement;
import de.hpi.accidit.orm.map.Mapping;

public abstract class Query<Result> {
	
	private static final Object[] NO_ARGS = {};

	private boolean frozen = false;
	protected final List<String> selectedFields = new ArrayList<>();
	private final StringBuilder sbSelect = new StringBuilder();
	private final StringBuilder sbFrom = new StringBuilder();
	private final StringBuilder sbWhere = new StringBuilder();
	private Object[] arguments = NO_ARGS;
	private int argC = 0;
	
	private final OConnection cnn;
	private final SelectBuilder selectBuilder;
	private final Mapping<Result> mapping;
	
	public Query(SelectBuilder selectBuilder, Mapping<Result> mapping) {
		this(null, selectBuilder, mapping);
	}
	
	public Query(OConnection cnn, SelectBuilder selectBuilder, Mapping<Result> mapping) {
		this.cnn = cnn;
		this.selectBuilder = selectBuilder;
		this.mapping = mapping;
	}
	
	protected void ensureNotFrozen() {
		if (frozen) {
			throw new IllegalStateException("Modification not allowed");
		}
	}
	
	protected void select(String... fields) {
		ensureNotFrozen();
		if (sbSelect.length() > 0) {
			sbSelect.append(", ");
		}
		selectBuilder.appendFields(sbSelect, fields);
		selectedFields.addAll(selectBuilder.fieldKeys(fields));
	}
	
	protected void from(String table) {
		ensureNotFrozen();
		if (sbFrom.length() > 0) {
			sbFrom.append(", ");
		}
		sbFrom.append(table);
	}
	
	protected void from(final String... tables) {
		ensureNotFrozen();
		for (int i = 0; i < tables.length; i++) {
			if (i > 0 || sbFrom.length() > 0) {
				sbFrom.append(", ");
			}
			sbFrom.append(tables[i]);
		}
	}
	
	protected void join(String table, String condition) {
		ensureNotFrozen();
		sbFrom.append(" JOIN ");
		sbFrom.append(table);
		sbFrom.append(" ON ");
		sbFrom.append(condition);
	}
	
	protected void where(String condition) {
		ensureNotFrozen();
		if (sbWhere.length() > 0) {
			sbWhere.append(" AND ");
		}
		sbWhere.append(condition);
	}
	
	protected void setArgument(int i, Object value) {
		if (argC <= i) {
			int cap = arguments.length;
			if (cap <= i) {
				if (cap < 8) cap = 8;
				while (cap <= i) cap *= 2;
				arguments = Arrays.copyOf(arguments, cap);
			}
			argC = i+1;
		}
		arguments[i] = value;
	}
	
	protected void addArgument(Object value) {
		setArgument(argC, value);
	}
	
	protected String queryString() {
		StringBuilder sb = new StringBuilder("SELECT ");
		sb.append(sbSelect)
		  .append(" FROM ")
		  .append(sbFrom);
		if (sbWhere.length() > 0) {
			sb.append(" WHERE ")
			  .append(sbWhere);
		}
		return sb.toString();
	}

	public Submit<Result> submit(OConnection cnn) throws SQLException {
		frozen = true;
		OPreparedStatement ps = cnn.prepare(queryString());
		OFuture<ResultSet> result = ps.submit(Arrays.copyOf(arguments, argC));
		return new Submit<>(result, selectedFields, mapping);
	}
	
	public Submit<Result> submit() throws SQLException {
		return submit(cnn);
	}
	
	public Run<Result> run(OConnection cnn) throws SQLException {
		frozen = true;
		OPreparedStatement ps = cnn.prepare(queryString());
		ResultSet result = ps.run(Arrays.copyOf(arguments, argC));
		return new Run<Result>(result, selectedFields, mapping);
	}
	
	public Run<Result> run() throws SQLException {
		return run(cnn);
	}
	
}
