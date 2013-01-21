package de.hpi.accidit.eclipse.views.elements;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.hpi.accidit.eclipse.DatabaseConnector;

public class LocalsContentProvider implements ITreeContentProvider {
	
	private Connection dbConnection;
	
	private static int currentTestId = 2;
	
	private boolean nothingSelected = true;
	private int currentMethodId;
	private long currentMethodStep;
	
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
	
	public void calledMethodSelected(CalledMethod method) {
		nothingSelected = false;
		currentMethodId = method.methodId;
		currentMethodStep = method.callStep;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if(nothingSelected) {
			return new Object[0];
		}
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT methodId, variableId, variable, arg, primType, valueId, step ");
		query.append("FROM vvariabletrace ");
		query.append("WHERE testId = " + currentTestId + " ");
		query.append("AND methodId = " + currentMethodId + " ");
		query.append("ORDER BY variable");
		
		return queryForLocals(query.toString()).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		Local local = (Local) element;
		return local.isObject();
	}
	
	private List<Local> queryForLocals(String query) {
		List<Local> locals = new LinkedList<Local>();
		
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
			while(result.next()) locals.add(buildLocal(result));
		} catch (SQLException e) {
			System.err.println("Failed to process result set. Exiting.");
			e.printStackTrace();
			System.exit(0);
		}
		
		return locals;
	}
	
	private Local buildLocal(ResultSet result) throws SQLException {
		Local local = new Local();
		local.methodId	= result.getInt(1);
		local.id		= result.getInt(2);
		local.name		= result.getString(3);
		local.arg		= result.getInt(4);
		local.primType	= result.getString(5).charAt(0);
		local.valueId	= result.getLong(6);
		local.step		= result.getInt(7);
		return local;
	}
}
