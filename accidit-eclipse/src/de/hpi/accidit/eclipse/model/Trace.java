package de.hpi.accidit.eclipse.model;

import static org.cthul.miro.DSL.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;

import de.hpi.accidit.eclipse.TraceNavigatorUI;
import de.hpi.accidit.eclipse.views.TraceExplorerView;

public class Trace {

	public final int id;
	public final TraceNavigatorUI ui;

	public TraceElement[] root;
	
	public Trace(int id, final TraceNavigatorUI ui) {
		this.id = id;
		this.ui = ui;
		try {
			root = select()
				.from(Invocation.VIEW)
				.where().rootOfTest(id)
				.execute(ui.cnn()).asArray();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			root = new TraceElement[0];
		}
	}
	
	public TraceElement[] getRootElements() {
		return root;
	}
	
	public TraceElement getStep(long step) {
		List<TraceElement> stack = getStack(step);
		if (stack.isEmpty()) return root[0];
		TraceElement te = stack.get(stack.size()-1);
		if (te.getStep() != step) {
			TraceElement match = te;
			te = new TraceElement(){{
				testId = match.testId;
				parent = match.parent;
				callStep = match.callStep;
				line = match.line;
			}};
			te.step = step;
		}
		return te;
	}
	
	public List<TraceElement> getStack(long step) {
		TraceElement[] elements = getRootElements();
		List<TraceElement> stack = new ArrayList<>();
		fillStack(step, elements, stack);
		return stack;
	}
	
	private void fillStack(long step, TraceElement[] elements, List<TraceElement> stack) {
		TraceElement currentElement = null;
		for (int i = 0; i < elements.length; i++) {
			currentElement = elements[i];
			if (currentElement.getStep() == step) {
				stack.add(currentElement);
				return;
			}
			if (currentElement.getStep() > step) {
				if (i > 0) {
					currentElement = elements[i - 1];
				}
				break;
			}
		}
		if (currentElement == null) return;
		stack.add(currentElement);
		if (currentElement instanceof Invocation) {
			Invocation inv = (Invocation) currentElement;
			if (inv.exitStep > step) {
				fillStack(step, inv.getChildren(), stack);
			}
		}
	}
}
