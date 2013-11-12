package de.hpi.accidit.eclipse.model.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cthul.miro.MiConnection;
import org.cthul.miro.at.Always;
import org.cthul.miro.at.Config;
import org.cthul.miro.at.More;
import org.cthul.miro.map.ConfigurationProvider;
import org.cthul.miro.map.Mapping;
import org.cthul.miro.result.EntityConfiguration;
import org.cthul.miro.result.EntityInitializer;

import de.hpi.accidit.eclipse.model.ModelBase;

public class ModelDaoBase {

	
	public static final ConfigurationProvider<ModelBase> SET_CONNECTION = new ConfigurationProvider<ModelBase>() {
		@Override
		public <E extends ModelBase> EntityConfiguration<? super E> getConfiguration(MiConnection cnn, Mapping<E> mapping) {
			return new SetMiConnection<>(cnn, mapping);
		}
	};
	
	private static class SetMiConnection<E extends ModelBase> implements EntityConfiguration<E>, EntityInitializer<E> {
		
		private final MiConnection cnn;
		private final Mapping<E> mapping;

		public SetMiConnection(MiConnection cnn, Mapping<E> mapping) {
			this.cnn = cnn;
			this.mapping = mapping;
		}

		@Override
		public void apply(E entity) throws SQLException {
			mapping.setField(entity, "cnn", cnn);
		}

		@Override
		public void complete() throws SQLException { }

		@Override
		public void close() throws SQLException { }

		@Override
		public EntityInitializer<E> newInitializer(ResultSet rs) throws SQLException {
			return this;
		}
	}
	
	@Always(@More(
	config = @Config(impl=ModelDaoBase.class,factory="SET_CONNECTION")		
	))
	public static interface Query {
		
	}
	
}
