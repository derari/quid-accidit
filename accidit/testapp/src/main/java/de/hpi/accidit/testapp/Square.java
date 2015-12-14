package de.hpi.accidit.testapp;

import de.hpi.accidit.testapp.Geometry.Shape2D;

class Square implements Shape2D {
	private double length;
	
	public Square(double length) {
		this.length = length;
	}
	
	@Override
	public double getArea() {
		return length * length;
	}
}