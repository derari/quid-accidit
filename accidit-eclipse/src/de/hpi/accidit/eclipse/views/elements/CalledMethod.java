package de.hpi.accidit.eclipse.views.elements;


public class CalledMethod {

	public int testId;
	public long callStep;
	public int exitStep;
	public int depth;
	public int callLine;
	
	public int methodId;
	public String type;
	public String method;
	
	public CalledMethod parentMethod;

	public CalledMethod() { };
}
