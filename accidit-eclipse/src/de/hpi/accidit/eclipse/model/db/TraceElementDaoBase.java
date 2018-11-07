package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.map.MappingKey;
import org.cthul.miro.map.layer.MappedQuery;
import org.cthul.miro.request.impl.SnippetTemplateLayer;
import org.cthul.miro.request.template.TemplateLayer;
import org.cthul.miro.sql.SelectQuery;
import org.cthul.miro.sql.set.MappedSqlBuilder;

import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.TraceElement;

public abstract class TraceElementDaoBase<Entity extends TraceElement, This extends TraceElementDaoBase<Entity, This>> extends ModelDaoBase<Entity, This> {
	
	protected static void init(MappedSqlBuilder<?,?> sql) {
		ModelDaoBase.init(sql);
		sql.sql("SELECT e.`testId`, e.`line`, e.`step`");
	}
	
	protected TraceElementDaoBase(ModelDaoBase<Entity, This> source) {
		super(source);
	}
	
	public TraceElementDaoBase(MiConnection cnn, TemplateLayer<MappedQuery<Entity, SelectQuery>> queryLayer) {
		super(cnn, queryLayer);
	}

	@Override
	protected void initializeSnippetLayer(SnippetTemplateLayer<MappedQuery<Entity, SelectQuery>> snippetLayer) {
		super.initializeSnippetLayer(snippetLayer);
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		setUp(MappingKey.FETCH, "testId", "line", "step", "callStep");
	}
	
	public This callStep(long callStep) {
		return setUp(MappingKey.PROPERTY_FILTER, "callStep", callStep);
	}
	
	public This inInvocation(Invocation inv) {
		return doSafe(me -> {
			me.initializeWith(te -> te.parent = inv);
			me.setUp(MappingKey.PROPERTY_FILTER, 
					"testId", inv.getTestId(), "callStep", inv.getStep());
		});
	}
	
	public This orderByStep() {
		return sql(sql -> sql.orderBy().sql("e.`step`"));
	}
}
