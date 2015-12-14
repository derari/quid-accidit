package de.hpi.accidit.testapp;

import de.hpi.accidit.testapp.Geometry.Shape2D;
import de.hpi.accidit.testapp.Geometry.Shape3D;

class Pyramid implements Shape3D {
	private Shape2D base;
	private double height;
	public Pyramid(Shape2D baseShape, double height) {
		base = baseShape;
		height = height;
	}
	
	@Override
	public double getVolume() {
		return base.getArea() * height / 3;
	}
}