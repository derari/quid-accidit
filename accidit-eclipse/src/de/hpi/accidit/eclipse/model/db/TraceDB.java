package de.hpi.accidit.eclipse.model.db;

import org.cthul.miro.db.MiConnection;
import org.cthul.miro.sql.set.MappedSqlSchema;

public class TraceDB {
//	
//	private final MiConnection cnn;
//	private final Graph graph;
	
	private final VariableEventDao variableEventDao;
	private final FieldEventDao fieldEventDao;
	private final ExceptionEventDao throwDao;
	private final ExceptionEventDao catchDao;
	
	private final InvocationDao invocationDao;
	private final ValueDao valueDao;
	private final ObjectOccurranceDao objectOccurranceDao;
	private final SideEffectsDao sideEffectsDao;
	
	private final FieldValueDao fieldValueDao;
	private final ArrayItemDao arrayItemDao;
	private final VariableValueDao variableValueDao;
	
	private final FieldDao fieldDao;
	private final MethodDao methodDao;
	private final VariableDao variableDao;
	private final TypeDao typeDao;

	public TraceDB(MiConnection cnn, String schema) {
		MappedSqlSchema schemaBuilder = new MappedSqlSchema();
		schemaBuilder.setDefaultSchema(schema);
//		this.cnn = cnn;
//		this.graph = schemaBuilder.newFakeGraph(cnn);
		
		VariableEventDao.init(schemaBuilder);
		this.variableEventDao = new VariableEventDao(cnn, schemaBuilder);
		
		FieldEventDao.init(schemaBuilder);
		this.fieldEventDao = new FieldEventDao(cnn, schemaBuilder);
		
		ExceptionEventDao.init(schemaBuilder);
		this.throwDao = new ExceptionEventDao(cnn, schemaBuilder, true);
		this.catchDao = new ExceptionEventDao(cnn, schemaBuilder, false);
		
		InvocationDao.init(schemaBuilder);
		this.invocationDao = new InvocationDao(cnn, schemaBuilder);

		ValueDao.init(schemaBuilder);
		this.valueDao = new ValueDao(cnn, schemaBuilder);
		
		ObjectOccurranceDao.init(schemaBuilder);
		this.objectOccurranceDao = new ObjectOccurranceDao(cnn, schemaBuilder);
		
		SideEffectsDao.init(schemaBuilder);
		this.sideEffectsDao = new SideEffectsDao(cnn, schemaBuilder);

		FieldValueDao.init(schemaBuilder);
		this.fieldValueDao = new FieldValueDao(cnn, schemaBuilder);
		
		ArrayItemDao.init(schemaBuilder);
		this.arrayItemDao = new ArrayItemDao(cnn, schemaBuilder);

		VariableValueDao.init(schemaBuilder);
		this.variableValueDao = new VariableValueDao(cnn, schemaBuilder);
		
		FieldDao.init(schemaBuilder);
		this.fieldDao = new FieldDao(cnn, schemaBuilder);
		
		MethodDao.init(schemaBuilder);
		this.methodDao = new MethodDao(cnn, schemaBuilder);
		
		VariableDao.init(schemaBuilder);
		this.variableDao = new VariableDao(cnn, schemaBuilder);
		
		TypeDao.init(schemaBuilder);
		this.typeDao = new TypeDao(cnn, schemaBuilder);
	}
	
	public VariableEventDao variableEvents() {
		return variableEventDao;
	}	
	
	public FieldEventDao fieldEvents() {
		return fieldEventDao;
	}
	
	public ExceptionEventDao throwEvents() {
		return throwDao;
	}
	
	public ExceptionEventDao catchEvents() {
		return catchDao;
	}
	
	public InvocationDao invocations() {
		return invocationDao;
	}
	
	public ValueDao values() {
		return valueDao;
	}
	
	public ObjectOccurranceDao objectOccurrances() {
		return objectOccurranceDao;
	}
	
	public SideEffectsDao sideEffects() {
		return sideEffectsDao;
	}
	
	public FieldValueDao fieldValues() {
		return fieldValueDao;
	}
	
	public ArrayItemDao arrayValues() {
		return arrayItemDao;
	}
	
	public VariableValueDao variableValues() {
		return variableValueDao;
	}
	
	public FieldDao fields() {
		return fieldDao;
	}
	
	public MethodDao methods() {
		return methodDao;
	}
	
	public VariableDao variables() {
		return variableDao;
	}
	
	public TypeDao types() {
		return typeDao;
	}
}
