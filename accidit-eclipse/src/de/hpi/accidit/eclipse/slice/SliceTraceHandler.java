package de.hpi.accidit.eclipse.slice;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

public class SliceTraceHandler extends AbstractHandler implements IHandler {
	
	private static final String TYPE_PARAMETER_ID = "de.hpi.accidit.eclipse.commands.sliceTrace.type";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
//		ITreeSelection sel = (ITreeSelection) TraceNavigatorUI.getGlobal().getTraceExplorer().getSelection();
//		if (sel == null || sel.isEmpty()) return null;
//		
//		NamedValueNode selection = (NamedValueNode) sel.getFirstElement();
//		if (selection == null) return null;
//		
//		NamedValue value = selection.getValue();
		
		
		// contains the value of the command's type parameter
		// values: "call_only", "arguments_only", "call_and_arguments"
		String type = event.getParameter(TYPE_PARAMETER_ID); 
		
		System.out.println(String.format("SliceTraceHandler command with parameter %s executed.", type));
		
		return null;
	}

}
