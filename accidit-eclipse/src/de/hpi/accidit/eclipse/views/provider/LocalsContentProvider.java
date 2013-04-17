package de.hpi.accidit.eclipse.views.provider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.views.dataClasses.Method;
import de.hpi.accidit.eclipse.views.dataClasses.LocalBase;
import de.hpi.accidit.eclipse.views.dataClasses.LocalObject;
import de.hpi.accidit.eclipse.views.dataClasses.LocalPrimitive;

public class LocalsContentProvider implements ITreeContentProvider {
		
	private Connection dbConnection;
	
	private int selectedTestCaseId;
	private Method selectedMethod;
	
	public LocalsContentProvider() {
		try {
			dbConnection = DatabaseConnector.getValidConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("No database connection available. Exiting.");
			System.exit(0);
		}
	}

	@Override
	public void dispose() { }

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { }
	
	public void setSelectedMethod(Method method) {
		// TODO refactor into setSelectedMethod and setTestId
		
		this.selectedMethod = method;
		
		if(method != null) this.selectedTestCaseId = method.testId;		
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if(selectedMethod == null) return new Object[0];
		if(selectedMethod.parentMethod == null) return new Object[0];
		
		// TODO: fix, currently not the actual type but the declared one.
		// TODO: evaluate if callStep or exitStep is interesting here!
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT v.id, v.name, vt.primType, vt.valueId, vt.step, t.id, t.name ");
		query.append("FROM VariableTrace vt ");
		query.append("JOIN Variable v ON v.id = vt.variableId ");
		query.append("JOIN Type t ON t.id = v.typeId ");
		query.append("WHERE vt.testId = " + selectedTestCaseId + " ");
		query.append("AND vt.step <= " + selectedMethod.callStep + " ");
		query.append("AND vt.methodId = " + selectedMethod.parentMethod.methodId + " ");
		query.append("AND v.methodId = " + selectedMethod.parentMethod.methodId + " ");
		query.append("ORDER BY v.name");
		
		System.out.println("Querying getElements: " + query.toString());
		
		return queryForLocals(query.toString(), true).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof LocalObject)) return null;
		
		LocalObject local = (LocalObject) parentElement;
		StringBuilder query = new StringBuilder();
		query.append("SELECT f.Id, f.name, pt.primType, pt.valueId, pt.step, t.id, t.name ");
		query.append("FROM Field f ");
		query.append("LEFT OUTER JOIN (");
		query.append("	SELECT step, fieldId, primType, valueId ");
		query.append("	FROM PutTrace ");
		query.append("	WHERE testId = " + selectedTestCaseId + " ");
		query.append("	AND thisId = " + local.objectId + " ");
		query.append("	AND step <= " + selectedMethod.callStep + " ");
		query.append(") pt ON pt.fieldId = f.id ");
		query.append("JOIN Type t ON t.id = f.typeId ");
		query.append("WHERE f.declaringTypeID = " + local.typeId);
		
		System.out.println("Querying getChildren: " + query.toString());
		
		return queryForLocals(query.toString(), false).toArray();
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		LocalBase local = (LocalBase) element;
		return local.isObject();
	}
	
	private List<LocalBase> queryForLocals(String query, boolean areLocalVariables) {
		List<LocalBase> locals = new LinkedList<LocalBase>();
		
		ResultSet result = null;
		try {
			Statement statement = dbConnection.createStatement();
			result = statement.executeQuery(query);
		} catch (SQLException e) {
			System.err.println("Locals not retrievable. Exiting.");
			e.printStackTrace();
			System.exit(0);
		}
		
		try {
			while(result.next()) locals.add(buildLocal(result, areLocalVariables));
		} catch (SQLException e) {
			System.err.println("Failed to process result set. Exiting.");
			e.printStackTrace();
			System.exit(0);
		}
		
		return locals;
	}
	
	private LocalBase buildLocal(ResultSet result, boolean isLocalVariable) throws SQLException {
		LocalBase local = createLocal(result);
		setLocalBaseFields(local, result);
		local.isLocalVariable = isLocalVariable;
		return local;
	}
	
	private LocalBase createLocal(ResultSet result) throws SQLException {
		if(result.getString(3) == null) return new LocalPrimitive("null");
		
		char primType = result.getString(3).charAt(0);
		long valueId = result.getLong(4);
		if(primType == 'L') return new LocalObject(valueId);
		
		switch(primType) {
		case 'Z': return new LocalPrimitive(String.valueOf(valueId == 1)); // boolean
		case 'B': return new LocalPrimitive(String.valueOf((byte) valueId)); // byte
		case 'C': return new LocalPrimitive(String.valueOf((char) valueId)); // char
		case 'D': return new LocalPrimitive(String.valueOf(Double.longBitsToDouble(valueId))); // double
		case 'F': return new LocalPrimitive(String.valueOf(Float.intBitsToFloat((int) valueId))); // float
		case 'I': return new LocalPrimitive(String.valueOf(valueId)); // int
		case 'J': return new LocalPrimitive(String.valueOf(valueId)); // long
		case 'S': return new LocalPrimitive(String.valueOf((short) valueId)); // short
		default: return null;
		}
	}

	private void setLocalBaseFields(LocalBase local, ResultSet result) throws SQLException {		
		local.id		= result.getInt(1);
		local.name		= result.getString(2);
		local.step		= result.getInt(5);
		local.typeId	= result.getInt(6);
		local.type		= result.getString(7);
	}
}
