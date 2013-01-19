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
	
	public CalledMethodContentProvider() {
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

	@Override
	public Object[] getElements(Object inputElement) {
		StringBuilder query = new StringBuilder();
		query.append("SELECT testId, callStep, exitStep, depth, callLine, methodId, type, method ");
		query.append("FROM vinvocationtrace ");
		query.append("WHERE testId = 0 ");
		query.append("AND depth = 0 ");
		query.append("ORDER BY callStep");
		
		return queryForCalledMethods(query.toString()).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		CalledMethod calledMethod = (CalledMethod) parentElement;
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		query.append("testId, callStep, exitStep, depth, callLine, methodId, type, method ");
		query.append("FROM vinvocationtrace ");
		query.append("WHERE testId = 0 ");
		query.append("AND depth = " + (calledMethod.depth + 1) + " ");
		query.append("AND callStep > " + calledMethod.callStep + " ");
		query.append("AND exitStep < " + calledMethod.exitStep + " ");
		query.append("ORDER BY callStep");
		
		return queryForCalledMethods(query.toString()).toArray();
	}

	@Override
	public Object getParent(Object element) {
		CalledMethod calledMethod = (CalledMethod) element;
		
		if (calledMethod.depth == 0)
			return null;
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		query.append("testId, callStep, exitStep, depth, callLine, methodId, type, method ");
		query.append("FROM vinvocationtrace ");
		query.append("WHERE testId = 0 ");
		query.append("AND depth = " + (calledMethod.depth - 1) + " ");
		query.append("AND callStep < " + calledMethod.callStep + " ");
		query.append("AND exitStep > " + calledMethod.exitStep + " ");
		query.append("ORDER BY callStep");
		
		return queryForCalledMethods(query.toString()).toArray();
	}

	@Override
	public boolean hasChildren(Object element) {
		CalledMethod calledMethod = (CalledMethod) element;
		
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		query.append("testId, callStep, exitStep, depth, callLine, methodId, type, method ");
		query.append("FROM vinvocationtrace ");
		query.append("WHERE testId = 0 ");
		query.append("AND depth = " + (calledMethod.depth + 1) + " ");
		query.append("AND callStep > " + calledMethod.callStep + " ");
		query.append("AND exitStep < " + calledMethod.exitStep + " ");
		query.append("LIMIT 1");
		
		List<CalledMethod> result = queryForCalledMethods(query.toString());
		return !result.isEmpty();
	}
	
	private List<CalledMethod> queryForCalledMethods(String query) {
		
		// TODO remove debug print
		System.out.println("Querying: " + query);
		
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
				calledMethods.add(buildCalledMethod(result));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Failed to process result set. Exiting.");
			System.exit(0);
		}
		
		return calledMethods;
	}
	
	private CalledMethod buildCalledMethod(ResultSet result) throws SQLException {
		CalledMethod method = new CalledMethod();
		method.testId		= result.getInt(1);
		method.callStep		= result.getLong(2);
		method.exitStep		= result.getInt(3);
		method.depth		= result.getInt(4);
		method.callLine		= result.getInt(5);
		method.methodId		= result.getInt(6);
		method.type			= result.getString(7);
		method.method		= result.getString(8);
		return method;
	}
}
