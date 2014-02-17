package de.hpi.accidit.eclipse.model;

import java.util.Arrays;

import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;
import de.hpi.accidit.eclipse.model.Value.Primitive;

public class ValueToString {

	public static String getLongName(ObjectSnapshot v, NamedValue[] children) {
		String typeName = v.getTypeName();
		if (typeName == null) return simpleLongName(v);
		Integer aLen = v.getArrayLength();
		if (aLen != null) {
			int i = typeName.indexOf('[');
			if (i < 0) {
				return typeName + "[" + aLen + "] #" + v.getThisId();
			}
			return typeName.substring(0,i+1)
					+ aLen
					+ typeName.substring(i+1)
					+ " #" + v.getThisId();
		}
		switch (typeName) {
		case "java.lang.Byte":
		case "java.lang.Integer":
		case "java.lang.Long":
		case "java.lang.Short":
			return integerValue(v, children);
		case "java.lang.String":
			return stringValue(v, children);
		}
		if (typeName.startsWith("java.util")) {
			return javaUtilName(v, children);
		}
		return simpleLongName(v);
	}

	private static String simpleLongName(ObjectSnapshot v) {
		return v.getTypeName() + " #" + v.getThisId();
	}
	
	private static String simpleShortName(ObjectSnapshot v) {
		String s = v.getTypeName();
		int d = s.lastIndexOf('.');
		if (d >= 0) s = s.substring(d+1);
		return s + " #" + v.getThisId();
	}

	private static String stringValue(ObjectSnapshot v, NamedValue[] children) {
		ObjectSnapshot vValue = null;
		Primitive vCount = null, vOffset = null; 
		for (NamedValue nv: children) {
			switch (nv.getName()) {
			case "count":
				vCount = (Primitive) nv.getValue();
				break;
			case "offset":
				vOffset = (Primitive) nv.getValue();
				break;
			case "value":
				if (nv.getValue() instanceof Primitive) {
					System.out.println(
							"Error? Primitive 'value' in " + simpleLongName(v) +": " + simpleLongName(v));
				}
				vValue = (ObjectSnapshot) nv.getValue();
				break;
			}
		}
		if (vValue == null) {
			return simpleLongName(v);
		}
		int len = vValue.getArrayLength();
		char[] array = new char[len];
		Arrays.fill(array, '?');
		for (NamedValue c: vValue.getChildren()) {
			int i = Integer.parseInt(c.getName());
			array[i] = c.getValue().getShortString().charAt(0);
		}
		
		int offset = vOffset != null ? (int) vOffset.getValueId() : 0;
		int count = vCount != null ? (int) vCount.getValueId() : (len - offset);
		
		String s = new String(array, offset, count);
		if (s.length() > 35) s = s.substring(0, 32) + "...";
		return "\"" + s + "\" (" + simpleShortName(v) + ")";
	}
	
	private static String integerValue(ObjectSnapshot v, NamedValue[] children) {
		String val = getValueShortString(children, "value");
		if (val == null) {
			return simpleLongName(v);
		}
		return val + " (" + simpleShortName(v) + ")";
	}
	
	private static String javaUtilName(ObjectSnapshot v, NamedValue[] children) {
		String name = v.getTypeName() + " #" + v.getThisId();
		String size = null;
		if (name.startsWith("java.util.Collections$")) {
			name = name.substring(22);
			if (name.startsWith("Empty")) {
				size = "0";
			}
		}
		if (size == null) size = getSize(v);
		if (size == null) {
			return name;
		}
		int i = name.lastIndexOf('.');
		if (i >= 0) name = name.substring(i); 
		return name + " (size = " + size + ")";
	}
	
	private static String getSize(Value collection) {
		String size = null;
		while (collection != null && size == null) {
			NamedValue[] c = collection.getChildren();
			size = getValueShortString(c, "size");
			if (size == null) {
				// collection is just a delegator, find actual
				collection = getValue(c, "map", "c", "list");
			}
		}
		return size;
	}
	
	private static String getValueShortString(NamedValue[] children, String name) {
		Value v = getValue(children, name);
		if (v == null) return null;
		return v.getShortString();
	}

	private static Value getValue(NamedValue[] children, String name) {
		for (NamedValue nv: children) {
			if (name.equals(nv.getName())) {
				return nv.getValue();
			}
		}
		return null;
	}
	
	private static Value getValue(NamedValue[] children, String... names) {
		for (NamedValue nv: children) {
			for (String n: names) {
				if (n.equals(nv.getName())) {
					return nv.getValue();
				}
			}
		}
		return null;
	}
}
