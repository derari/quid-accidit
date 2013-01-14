package de.hpi.accidit.model;

public enum PrimitiveType {
    
    OBJECT,
    BOOLEAN,
    BYTE,
    CHAR,
    DOUBLE,
    FLOAT,
    INT,
    LONG,
    SHORT,
    VOID;

    private final int id;

    private PrimitiveType() {
        this.id = ordinal();
    }

    public int getId() {
        return id;
    }
    
    public char getKey() {
        switch (this) {
            case OBJECT: return 'L';
            case BOOLEAN: return 'Z';
            case BYTE: return 'B';
            case CHAR: return 'C';
            case DOUBLE: return 'D';
            case FLOAT: return 'F';
            case INT: return 'I';
            case LONG: return 'J';
            case SHORT: return 'S';
            case VOID: return 'V';
        }
        return '?';
    }
    
    public static PrimitiveType resultOfMethod(String desc) {
        int i = desc.indexOf(')');
        if (i < 0) throw new IllegalArgumentException(
                "Expected method descriptor, got \"" + desc + "\"");
        return forDescriptor(desc, i+1);
    }
    
    public static PrimitiveType forDescriptor(String desc) throws IllegalArgumentException {
        return forDescriptor(desc, 0);
    }
    
    public static PrimitiveType forDescriptor(String desc, int i) throws IllegalArgumentException {
        PrimitiveType pt = forDescriptor(desc.charAt(i));
        if (pt == null)
            throw new IllegalArgumentException(
                        "Unexpected descriptor: " + desc.substring(i));
        return pt;
    }
    
    public static PrimitiveType forDescriptor(char c) throws IllegalArgumentException {
        switch (c) {
            case '[':
            case 'L': return OBJECT;
            case 'Z': return BOOLEAN;
            case 'B': return BYTE;
            case 'C': return CHAR;
            case 'D': return DOUBLE;
            case 'F': return FLOAT;
            case 'I': return INT;
            case 'J': return LONG;
            case 'S': return SHORT;
            case 'V': return VOID;
            default: return null;
        }
    }
    
    public static PrimitiveType forClass(String name) {
        switch (name) {
            case "boolean": return BOOLEAN;
            case "byte": return BYTE;
            case "char": return CHAR;
            case "double": return DOUBLE;
            case "float": return FLOAT;
            case "int": return INT;
            case "long": return LONG;
            case "short": return SHORT;
            case "void": return VOID;
            default: return OBJECT;
        }
    }

    public long toValueId(Object value) {
        if (value == null) {
            if (this == VOID) return 0;
            throw new NullPointerException(this.toString());
        }
        switch (this) {
            case OBJECT: 
                throw new UnsupportedOperationException("No IDs for objects");
            case CHAR:
                if (value instanceof Character) {
                    return ((Character) value).charValue();
                }
            case BOOLEAN:
                if (value instanceof Boolean) {
                    boolean b = (Boolean) value;
                    return b ? 1 : 0;
                }
            case BYTE:
            case INT:
            case LONG:
            case SHORT:
                return ((Number) value).longValue();
            case DOUBLE:
                return Double.doubleToRawLongBits((Double) value);
            case FLOAT:
                return Float.floatToRawIntBits((Float) value);
            case VOID:
                if (value != null) 
                    throw new IllegalArgumentException("Void must be null, was " + value);
                return 0;
        }
        throw new AssertionError();
    }
    
    public String toString(long valueId) {
        switch (this) {
            case DOUBLE: return String.format("%s%f", getKey(), Double.longBitsToDouble(valueId));
            case FLOAT: return String.format("%s%f", getKey(), Float.intBitsToFloat((int) valueId));
        }
        return String.format("%s%d", getKey(), valueId);
    }
    
}
