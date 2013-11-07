package de.hpi.accidit.eclipse.model;

import org.cthul.miro.MiConnection;

public class SideEffects extends ModelBase {

	private final long captureStart;
	private final long captureEnd;
	private final long targetStart;
	private final long targetEnd;
	
	public SideEffects(MiConnection cnn, long start, long end) {
		super(cnn);
		this.captureStart = start;
		this.captureEnd = end;
		this.targetStart = captureEnd+1;
		this.targetEnd = Long.MAX_VALUE;
	}

	public SideEffects(MiConnection cnn, long captureStart, long captureEnd, long targetStart, long targetEnd) {
		super(cnn);
		this.captureStart = captureStart;
		this.captureEnd = captureEnd;
		this.targetStart = targetStart;
		this.targetEnd = targetEnd;
	}
	
	
	

}
