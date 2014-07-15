package de.hpi.accidit.eclipse.slice;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ITreeSelection;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.model.Invocation;
import de.hpi.accidit.eclipse.model.TraceElement;

public class SliceTraceHandler extends AbstractHandler implements IHandler {
	
	private static final String TYPE_PARAMETER_ID = "de.hpi.accidit.eclipse.commands.sliceTrace.type";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ITreeSelection sel = (ITreeSelection) TraceNavigatorUI.getGlobal().getTraceExplorer().getSelection();
		if (sel == null || sel.isEmpty()) return null;
		
		TraceElement selection = (TraceElement) sel.getFirstElement();
		if (selection == null) return null;
		
		// contains the value of the command's type parameter
		// values: "call_only", "arguments_only", "call_and_arguments"
		String type = event.getParameter(TYPE_PARAMETER_ID);
		
		SlicingCriteriaView slicingCriteriaView = TraceNavigatorUI.getGlobal().getSlicingCriteriaView();
		
		if (selection instanceof Invocation) {
			Invocation invocation = (Invocation) selection;
			if (type.contains("call")) {
				slicingCriteriaView.addInvocation(invocation);
			}
			if (type.contains("arguments")) {
				slicingCriteriaView.addInvocationArguments(invocation);
			}
		}
		
		return null;
	}

}
