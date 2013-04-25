package de.hpi.accidit.orm.dsl;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.hpi.accidit.orm.OConnection;
import de.hpi.accidit.orm.OFuture;
import de.hpi.accidit.orm.OFutureAction;
import de.hpi.accidit.orm.map.ResultBuilder;
import de.hpi.accidit.orm.map.ResultBuilder.EntityFactory;

public class Submittable<R> {
	
	private final OConnection cnn;
	private final QueryBuilder<?> qb;
	private final RA<R, ?> ra;
	
	
	public <E> Submittable(OConnection cnn, QueryBuilder<E> qb,
			ResultBuilder<R, E> ra, EntityFactory<E> ef) {
		super();
		this.cnn = cnn;
		this.qb = qb;
		this.ra = new RA<R, E>(qb, ra, ef);
	}

	public R run(OConnection cnn) throws SQLException {
		ResultSet rs = qb.run(cnn);
		return ra.adapt(rs, cnn);
	}
	
	public R run() throws SQLException {
		return run(cnn);
	}

	public R _run(OConnection cnn) {
		try {
			return run(cnn);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public R _run() {
		try {
			return run();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public OFuture<R> submit(final OConnection cnn) throws SQLException {
		OFuture<ResultSet> rs = qb.submit(cnn);
		return rs.onComplete(new OFutureAction<OFuture<ResultSet>, R>() {
			@Override
			public R call(OFuture<ResultSet> result) throws Exception {
				return ra.adapt(result.get(), cnn);
			}
		});
	}
	
	public OFuture<R> submit() throws SQLException {
		return submit(cnn);
	}
	
	public OFuture<R> _submit(OConnection cnn) {
		try {
			return submit(cnn);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public OFuture<R> _submit() {
		try {
			return submit();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static class RA<R, E> {
		private final QueryBuilder<E> qb;
		private final ResultBuilder<R, E> ra;
		private final EntityFactory<E> ef;
		public RA(QueryBuilder<E> qb, ResultBuilder<R, E> ra, EntityFactory<E> ef) {
			super();
			this.qb = qb;
			this.ra = ra;
			this.ef = ef;
		}
		public R adapt(ResultSet rs, OConnection cnn) throws SQLException {
			return ra.adapt(rs, ef, qb.buildValueAdapter(cnn));
		}
	}
	
}
