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
            case BOOLEAN: return 'X';
            case BYTE: return 'B';
            case CHAR: return 'C';
            case DOUBLE: return 'D';
            case FLOAT: return 'F';
            case INT: return 'I';
            case LONG: return 'L';
            case SHORT: return 'S';
            case VOID: return 'V';
        }
        return '?';
    }
    
    public static PrimitiveType resultOfMethod(String desc) {
        int i = desc.indexOf(')');
        if (i < 0) throw new IllegalArgumentException(
                "Expected method descriptor, got \"" + desc + "\"");
        switch (desc.charAt(i+1)) {
            case 'L': return OBJECT;
            case 'X': return BOOLEAN;
            case 'B': return BYTE;
            case 'C': return CHAR;
            case 'D': return DOUBLE;
            case 'F': return FLOAT;
            case 'I': return INT;
            case 'J': return LONG;
            case 'S': return SHORT;
            case 'V': return VOID;
            default:
                throw new IllegalArgumentException(
                        "Unexpected descriptor: " + desc.substring(i+1));
        }
    }

    public long toValueId(Object value) {
        switch (this) {
            case OBJECT: 
                throw new UnsupportedOperationException("No IDs for objects");
            case BOOLEAN:
                boolean b = (Boolean) value;
                return b ? 1 : 0;
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
    
}
