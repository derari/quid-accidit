package de.hpi.accidit.eclipse.handlers.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import de.hpi.accidit.eclipse.DatabaseConnector;

public class TestCaseSelectionDialog extends ElementTreeSelectionDialog {

	public TestCaseSelectionDialog(Shell parent,
			ILabelProvider labelProvider, 
			ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(750, 500);
	}
	
	@Override
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		Composite treeComposite = new Composite(parent, SWT.NONE);
		treeComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		TreeViewer viewer = super.doCreateTreeViewer(treeComposite, style);
		Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);

		TreeColumn column0 = new TreeColumn(tree, SWT.LEFT);
		column0.setText("Name");
		TreeColumn column1 = new TreeColumn(tree, SWT.RIGHT);
		column1.setText("Id");
		
		TreeColumnLayout layout = new TreeColumnLayout();
		treeComposite.setLayout(layout);
		layout.setColumnData(column0, new ColumnWeightData(90, 100));
		layout.setColumnData(column1, new ColumnPixelData(50));
		
		return viewer;
	}
	
	public static class TestCaseSelectionContentProvider implements ITreeContentProvider {
		
		List<TestCase> testCases = null;

		@Override
		public void dispose() { }

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { }

		@Override
		public Object[] getElements(Object inputElement) {
//			if (!"input".equals(inputElement)) {
//				return null;
//			}
			if (testCases == null) {
				String query = getTestCaseQuery();
				ResultSet resultSet;
	
				try {
					resultSet = executeQuery(query);
					testCases = buildFromResultSet(resultSet);
				} catch (SQLException e) {
					e.printStackTrace();
					return null;
				}
			}
			return testCases.toArray();
		}
		
		private String getTestCaseQuery() {
			StringBuilder query = new StringBuilder();
			query.append("SELECT `id`, `name` ");
			query.append("FROM `SCHEMA`.`TestTrace` ");
			query.append("ORDER BY `id`");
			return query.toString();
		}
		
		private ResultSet executeQuery(String query) throws SQLException {
			ResultSet result = null;
//			try {
//				Statement statement = dbConnection.createStatement();
//				result = statement.executeQuery(preProcessedQuery);
//				statement.closeOnCompletion();
				result = DatabaseConnector.cnn().prepare(query).executeQuery(new Object[0]);
//			} catch (SQLException e) {
//				System.err.println("Variables not retrievable.");
//				e.printStackTrace();
//			}
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
	
	public static class TestCaseSelectionLabelProvider 
			extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if(!(element instanceof TestCase)) {
				System.err.println("Invalid Object in tree of class: " + element.getClass().getName());
				return null;
			}			

			TestCase testCase = (TestCase) element;
			switch(columnIndex) {
			case 1: return String.valueOf(testCase.id);
			case 0: return testCase.name;
			default: return null;
			}
		}

	}

}
