package de.hpi.accidit.eclipse.model;

import java.util.Arrays;

import de.hpi.accidit.eclipse.model.Value.ObjectSnapshot;

public class ValueToString {

	public static String getLongName(ObjectSnapshot v, NamedValue[] children) {
		String typeName = v.getTypeName();
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
		
		if (typeName.equals("java.lang.String")) {
			return stringValue(v, children);
		}
		return simpleLongName(v);
	}

	private static String simpleLongName(ObjectSnapshot v) {
		return v.getTypeName() + " #" + v.getThisId();
	}

	private static String stringValue(ObjectSnapshot v, NamedValue[] children) {
		ObjectSnapshot strValue = null;
		for (NamedValue nv: children) {
			if (nv.getName().equals("value")) {
				strValue = (ObjectSnapshot) nv.getValue();
				break;
			}
		}
		if (strValue == null) {
			return simpleLongName(v);
		}
		int len = strValue.getArrayLength();
		char[] array = new char[len];
		Arrays.fill(array, '?');
		for (NamedValue c: strValue.getChildren()) {
			int i = Integer.parseInt(c.getName());
			array[i] = c.getValue().getShortString().charAt(0);
		}
		String s = new String(array);
		if (s.length() > 35) s = s.substring(0, 32) + "...";
		return "\"" + s + "\" (String #" + v.getThisId() + ")";
	}
	
}
