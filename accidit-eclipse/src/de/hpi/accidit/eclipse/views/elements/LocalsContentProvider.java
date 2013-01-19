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

	@Override
	public Object[] getElements(Object inputElement) {
		StringBuilder query = new StringBuilder();
		query.append("SELECT methodId, variableId, variable, arg, valueId, step ");
		query.append("FROM vvariabletrace ");
		query.append("WHERE testId = 0 ");
		query.append("AND methodId = 16 ");
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
		return false;
	}
	
	private List<Local> queryForLocals(String query) {
		
		// TODO remove debug print
		System.out.println("Querying: " + query);
		
		List<Local> locals = new LinkedList<Local>();
		
		ResultSet result = null;
		try {
			Statement statement = dbConnection.createStatement();
			result = statement.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Locals not retrievable. Exiting.");
			System.exit(0);
		}
		
		try {

			while(result.next()) {
				locals.add(buildLocal(result));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Failed to process result set. Exiting.");
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
		local.valueId	= result.getLong(5);
		local.step		= result.getInt(6);
		return local;
	}
}
