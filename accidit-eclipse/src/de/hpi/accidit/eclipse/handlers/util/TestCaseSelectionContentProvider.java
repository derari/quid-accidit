package de.hpi.accidit.eclipse.handlers.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import de.hpi.accidit.eclipse.DatabaseConnector;

public class TestCaseSelectionContentProvider implements ITreeContentProvider {

	@Override
	public void dispose() { }

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { }

	@Override
	public Object[] getElements(Object inputElement) {
		String query = getTestCaseQuery();
		ResultSet resultSet;
		List<TestCase> testCases;

		try {
			resultSet = executeQuery(query);
			testCases = buildFromResultSet(resultSet);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		return testCases.toArray();
	}
	
	private String getTestCaseQuery() {
		StringBuilder query = new StringBuilder();
		query.append("SELECT id, name ");
		query.append("FROM TestTrace ");
		query.append("ORDER BY id");
		return query.toString();
	}
	
	private ResultSet executeQuery(String query) throws SQLException {
		Connection dbConnection = DatabaseConnector.getValidConnection();
		ResultSet result = null;
		try {
			Statement statement = dbConnection.createStatement();
			result = statement.executeQuery(query);
		} catch (SQLException e) {
			System.err.println("Locals not retrievable.");
			e.printStackTrace();
		}
		return result;
	}
	
	private List<TestCase> buildFromResultSet(ResultSet resultSet) throws SQLException {
		List<TestCase> testCases = new LinkedList<TestCase>();
		while(resultSet.next()) {
			TestCase testCase = new TestCase();
			testCase.id = resultSet.getInt(1);
			testCase.name = resultSet.getString(2);
			testCases.add(testCase);
		}
		return testCases;
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
		return false;
	}

}
