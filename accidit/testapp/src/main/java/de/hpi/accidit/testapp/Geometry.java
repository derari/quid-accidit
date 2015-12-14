package de.hpi.accidit.testapp;

/**
 *
 * @author C5173086
 */
public class Geometry {
    static double computeVolume(double width, boolean square, double height, String type) {
        double area;
//        if (square) {
                area = squareArea(width);
//        } else {
//                area = computeCircleArea(width);
//        }
//        if (area > 0) {
        double volume = 0;
            if (type == "cone") {
                volume = coneVolume(area, height);
            }
        return volume;
//        } else {
//            return 0;
//        }
    }
    
    static double coneVolume(double diameter, boolean square, double height) {
        double area;
        if (square) {
                area = squareArea(diameter);
        } else {
                area = computeCircleArea(diameter);
        }
//        if (area > 0) {
        double volume = coneVolume(area, height);
        return volume;
//        } else {
//            return 0;
//        }
    }
    
    static interface Shape2D {
    	double getArea();
    }
    
    static interface Shape3D {
    	double getVolume();
    }
    
    static double squareArea(double width) {
        return width*width;
    }
    
    static double computeCircleArea(double width) {
        return width*width;
    }
    
    static double coneVolume(double area, double height) {
        double $h = height / 3;
        return area * height / 3;
    }
}
