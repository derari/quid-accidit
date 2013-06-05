package de.hpi.accidit.eclipse.handlers.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import de.hpi.accidit.eclipse.DatabaseConnector;
import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.NamedValue;
import de.hpi.accidit.eclipse.views.dataClasses.LocalBase;
import de.hpi.accidit.eclipse.views.dataClasses.LocalObject;
import de.hpi.accidit.eclipse.views.dataClasses.LocalPrimitive;
import de.hpi.accidit.eclipse.views.provider.LocalsContentProvider;

public class LocalsHistoryContentProvider extends LocalsContentProvider
		 implements ITreeContentProvider {
	
	private int testId;
	private long call;

	public LocalsHistoryContentProvider(TreeViewer viewer) {
		super(viewer);
	}
	
	@Override
	public void setStep(int testId, long call, long step) {
		this.testId = testId;
		this.call = call;
	}
	
	public void setRoot(NamedValue root) {
		this.root = root;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return new Object[1];
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasChildren(Object element) {
		throw new UnsupportedOperationException();
	}
	
	/*
	private int testId;
	private long callStep;
	private long step;
//	private Method selectedMethod;
	private NamedValue selectedLocal;
	
	public LocalsHistoryContentProvider(int selectedTestCaseId, NamedValue selectedLocal) {
		this.testId = selectedTestCaseId;
		this.callStep = TraceNavigatorUI.getGlobal().getCallStep();
		this.step = TraceNavigatorUI.getGlobal().getStep();
		this.selectedLocal = selectedLocal;
	}

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	@Override
	public Object[] getElements(Object inputElement) {
		StringBuilder query = new StringBuilder();
		
//		if(selectedLocal.isLocalVariable) {
//			query.append("SELECT v.id, v.name, vt.primType, vt.valueId, vt.step, t.id, t.name ");
//			query.append("FROM VariableTrace vt ");
//			query.append("JOIN Variable v ON v.id = vt.variableId AND v.methodId = vt.methodId ");
//			query.append("JOIN Type t ON t.id = v.typeId ");
//			query.append("WHERE vt.testId = " + selectedTestCaseId + " ");
//			query.append("AND vt.methodId = " + selectedMethod.parentMethod.methodId + " ");
//			query.append("AND vt.variableId = " + selectedLocal.id + " ");
//			query.append("ORDER BY vt.step");
//		} else {
//			query.append("SELECT f.id, f.name, pt.primType, pt.valueId, pt.step, t.id, t.name ");
//			query.append("FROM PutTrace pt ");
//			query.append("JOIN Field f ON f.id = pt.fieldId ");
//			query.append("JOIN Type t ON t.id = f.typeId ");
//			query.append("WHERE pt.testId = " + selectedTestCaseId + " ");
//			query.append("AND pt.fieldId = " + selectedLocal.id + " ");
//			query.append("ORDER BY pt.step");
//		}
		
		return queryForLocals(query.toString()).toArray();
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
//		query.append("	AND step <= " + selectedMethod.callStep + " ");
		query.append(") pt ON pt.fieldId = f.id ");
		query.append("JOIN Type t ON t.id = f.typeId ");
		query.append("WHERE f.declaringTypeID = " + local.typeId);
		
		System.out.println("Querying getChildren: " + query.toString());
		
		return queryForLocals(query.toString()).toArray();
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
*/
}
