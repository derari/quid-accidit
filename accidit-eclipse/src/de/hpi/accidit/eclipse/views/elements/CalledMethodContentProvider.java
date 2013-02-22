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

public class CalledMethodContentProvider implements ITreeContentProvider {
	
	private Connection dbConnection;
	
	private int currentTestCaseId = 0;
	
	public CalledMethodContentProvider() {
		try {
			dbConnection = DatabaseConnector.getValidConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("No database connection available. Exiting.");
			System.exit(0);
		}
	}
	
	public int getCurrentTestCaseId() {
		return currentTestCaseId;
	}
	
	public void setCurrentTestCaseId(int id) {
		currentTestCaseId = id;
	}

	@Override
	public void dispose() { }

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { }

	@Override
	public Object[] getElements(Object inputElement) {
		StringBuilder query = new StringBuilder();
		query.append("SELECT testId, callStep, exitStep, depth, callLine, methodId, type, method ");
		query.append("FROM vinvocationtrace ");
		query.append("WHERE testId = " + currentTestCaseId + " ");
		query.append("AND depth = 0 ");
		query.append("ORDER BY callStep");
		
		return queryForCalledMethods(query.toString(), null).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof CalledMethod)) return null;
				
		CalledMethod parentMethod = (CalledMethod) parentElement;
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		query.append("testId, callStep, exitStep, depth, callLine, methodId, type, method ");
		query.append("FROM vinvocationtrace ");
		query.append("WHERE testId = " + currentTestCaseId + " ");
		query.append("AND depth = " + (parentMethod.depth + 1) + " ");
		query.append("AND callStep > " + parentMethod.callStep + " ");
		query.append("AND exitStep < " + parentMethod.exitStep + " ");
		query.append("ORDER BY callStep");
		
		return queryForCalledMethods(query.toString(), parentMethod).toArray();
	}

	@Override
	public Object getParent(Object element) {
		if (!(element instanceof CalledMethod)) return null;
		
		CalledMethod calledMethod = (CalledMethod) element;
		return calledMethod.parentMethod;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (!(element instanceof CalledMethod)) return false;
		
		CalledMethod method = (CalledMethod) element;
		return getChildren(method).length != 0;
	}
	
	private List<CalledMethod> queryForCalledMethods(String query, CalledMethod parentMethod) {
		List<CalledMethod> calledMethods = new LinkedList<CalledMethod>();
		
		ResultSet result = null;
		try {
			Statement statement = dbConnection.createStatement();
			result = statement.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Root method calls not retrievable. Exiting.");
			System.exit(0);
		}
		
		try {
			while(result.next()) {
				calledMethods.add(buildCalledMethod(result, parentMethod));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Failed to process result set. Exiting.");
			System.exit(0);
		}
		
		return calledMethods;
	}
	
	private CalledMethod buildCalledMethod(ResultSet result, CalledMethod parentMethod) throws SQLException {
		CalledMethod method = new CalledMethod();
		method.testId		= result.getInt(1);
		method.callStep		= result.getLong(2);
		method.exitStep		= result.getInt(3);
		method.depth		= result.getInt(4);
		method.callLine		= result.getInt(5);
		method.methodId		= result.getInt(6);
		method.type			= result.getString(7);
		method.method		= result.getString(8);
		method.parentMethod = parentMethod;
		return method;
	}
}
