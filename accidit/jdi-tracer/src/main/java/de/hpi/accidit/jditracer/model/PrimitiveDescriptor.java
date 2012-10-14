package de.hpi.accidit.jditracer.model;

import com.sun.jdi.*;

/**
 *
 * @author Arian Treffer
 */
public class PrimitiveDescriptor {
    
    public static int primitiveTypeId(Value value) {
        if (value == null || value instanceof NullValue) return 0;
        Type t = value.type();
        switch (t.name()) {
            case "boolean":
            //case "java.lang.Boolean":
                return 1;
            case "byte":
            //case "java.lang.Byte":
                return 2;
            case "char":
            //case "java.lang.Character":
                return 3;
            case "double":
            //case "java.lang.Double":
                return 4;
            case "float":
            //case "java.lang.Float":
                return 5;
            case "int":
            //case "java.lang.Integer":
                return 6;
            case "long":
            //case "java.lang.Long":
                return 7;
            case "short":
            //case "java.lang.Short":
                return 8;
            case "void":
                return 9;
        }
        assert !(value instanceof PrimitiveValue) : 
                "value should be reference";
        return 0;
    }
    
    public static long primitiveData(Value value, int primTypeId) {
        if (primTypeId == 9) return 0; //void
        PrimitiveValue pValue = (PrimitiveValue) value;
        if (primTypeId == 5) {
            return Float.floatToIntBits(pValue.floatValue());
        }
        if (primTypeId == 4) {
            return Double.doubleToLongBits(pValue.doubleValue());
        }
        return pValue.longValue();
    }
    
}
