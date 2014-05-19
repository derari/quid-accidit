package de.hpi.accidit.eclipse.model.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cthul.miro.result.EntityBuilderBase;
import org.cthul.miro.result.EntityConfiguration;
import org.cthul.miro.result.EntityInitializer;

public abstract class SimpleEntityConfig<E> extends EntityBuilderBase implements EntityConfiguration<E> {

	@Override
	public EntityInitializer<E> newInitializer(ResultSet rs) throws SQLException {
		return new Init(rs);
	}
	
	protected abstract void apply(ResultSet rs, E entity) throws SQLException;

	protected void complete(ResultSet rs) throws SQLException {
	}

	protected void close(ResultSet rs) throws SQLException {
	}
	
	private class Init implements EntityInitializer<E> {

		private final ResultSet rs;
		
		public Init(ResultSet rs) {
			this.rs = rs;
		}
		
		@Override
		public void apply(E entity) throws SQLException {
			SimpleEntityConfig.this.apply(rs, entity);
		}

		@Override
		public void complete() throws SQLException {
			SimpleEntityConfig.this.complete(rs);
		}

		@Override
		public void close() throws SQLException {
			SimpleEntityConfig.this.close(rs);
		}
	}
}
