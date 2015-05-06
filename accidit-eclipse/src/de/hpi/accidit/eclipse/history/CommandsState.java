package de.hpi.accidit.eclipse.history;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

public class CommandsState extends AbstractSourceProvider {

	public final static String BACK_STATE = "de.hpi.accidit.eclipse.history.backState";
	public final static String FORWARD_STATE = "de.hpi.accidit.eclipse.history.forwardState";
	
	private boolean goBackAllowed = false;
	private boolean goForwardAllowed = false;

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { BACK_STATE, FORWARD_STATE };
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map getCurrentState() {
		Map map = new HashMap(2);
	    map.put(BACK_STATE, goBackAllowed);
	    map.put(FORWARD_STATE, goForwardAllowed);
	    return map;
	}
	
	public void setGoBackAllowed(boolean state) {
		goBackAllowed = state;
		fireSourceChanged(ISources.WORKBENCH, BACK_STATE, goBackAllowed);
	}
	
	public void setGoForwardAllowed(boolean state) {
		goForwardAllowed = state;
		fireSourceChanged(ISources.WORKBENCH, FORWARD_STATE, goForwardAllowed);
	}

	@Override
	public void dispose() { }
}
