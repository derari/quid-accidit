package de.hpi.accidit.eclipse.views;

import de.hpi.accidit.eclipse.model.TraceElement;

public interface AcciditView {
	
	public void setStep(TraceElement te, boolean before);
	
	public void sliceChanged();
}
