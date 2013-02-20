package de.hpi.accidit.eclipse.handlers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.views.LocalsExplorerView;
import de.hpi.accidit.eclipse.views.MethodExplorerView;
import de.hpi.accidit.eclipse.views.elements.CalledMethod;
import de.hpi.accidit.eclipse.views.elements.LocalBase;

public class BrowseVariableHistoryHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelectionService selectionService = HandlerUtil.getActiveWorkbenchWindow(event).getSelectionService();
		ITreeSelection selectedMethod = (ITreeSelection) selectionService.getSelection(MethodExplorerView.ID);
		ITreeSelection selectedLocal = (ITreeSelection) selectionService.getSelection(LocalsExplorerView.ID);
		
//		MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Handler in Place!", "Go on! >>> " + selection);
		
		if (selectedLocal.size() != 1) return null;
		LocalBase local = (LocalBase) selectedLocal.getFirstElement();
		CalledMethod method = (CalledMethod) selectedMethod.getFirstElement();
		
		ElementListSelectionDialog dialog = 
				new ElementListSelectionDialog(HandlerUtil.getActiveShell(event), new LabelProvider());
		dialog.setElements(getLocalHistory(local, method));
		dialog.setTitle("Which operating system are you using");
		// User pressed cancel
		if (dialog.open() != Window.OK) return false;
		Object[] result = dialog.getResult();
		
		return null;
	}
	
	private Object[] getLocalHistory(LocalBase local, CalledMethod method) {
		StringBuilder query = new StringBuilder();
		query.append("SELECT vt.primType, vt.valueId, vt.step ");
		query.append("FROM VariableTrace vt ");
		query.append("WHERE vt.testId = " + method.testId + " ");
		query.append("AND vt.methodId = " + method.parentMethod.methodId + " ");
		query.append("AND vt.variableId = " + local.id + " ");
		query.append("ORDER BY vt.step");

		System.out.println("Querying getLocalHistory: " + query.toString());
		
		return queryForLocalHistory(query.toString()).toArray();
	}
	
	private Connection getDbConnection() {
		try {
			return DatabaseConnector.getValidConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("No database connection available. Exiting.");
			System.exit(0);
		}
		return null;
	}
	
	private List<LocalHistory> queryForLocalHistory(String query) {
		List<LocalHistory> locals = new LinkedList<LocalHistory>();
		
		ResultSet result = null;
		try {
			Statement statement = getDbConnection().createStatement();
			result = statement.executeQuery(query);
		} catch (SQLException e) {
			System.err.println("Locals not retrievable. Exiting.");
			e.printStackTrace();
			System.exit(0);
		}
		
		try {
			while(result.next()) locals.add(buildLocalHistory(result));
		} catch (SQLException e) {
			System.err.println("Failed to process result set. Exiting.");
			e.printStackTrace();
			System.exit(0);
		}
		
		return locals;
	}
	
	private LocalHistory buildLocalHistory(ResultSet result) throws SQLException {
		LocalHistory local = new LocalHistory();
		local.primType = result.getString(1);
		local.valueId = result.getLong(2);
		local.step = result.getInt(3);
		return local;
	}
	
	class LocalHistory {
		int step;
		String primType;
		long valueId;
		
		@Override
		public String toString() {
			return String.format("%d: %d (%s)", step, valueId, primType);
		}
	}

}
