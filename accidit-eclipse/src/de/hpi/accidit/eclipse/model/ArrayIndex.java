package de.hpi.accidit.eclipse.model;

public class ArrayIndex implements NamedEntity {
	
	public static ArrayIndex[] newIndexArray(int len) {
		ArrayIndex[] array = new ArrayIndex[len];
		for (int i = 0; i < len; i++) {
			array[i] = new ArrayIndex(i);
		}
		return array;
	}

	private final int index;
	
	public ArrayIndex(int index) {
		this.index = index;
	}

	@Override
	public int getId() {
		return index;
	}
	
	@Override
	public String getName() {
		return "[" + getId() + "]";
	}
}
