package de.hpi.accidit.eclipse.slice;

import java.util.HashMap;
import java.util.Map;

import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;

public class CodeIndex implements Comparable<CodeIndex> {
	
	public static CodeIndex anyAtLine(int line) {
		return new CodeIndex(line, -1);
	}

	private final int line;
	private final int lineIndex;
	
	public CodeIndex(int line, int lineIndex) {
		super();
		this.line = line;
		this.lineIndex = lineIndex;
	}
	
	public int getLine() {
		return line;
	}
	
	public int getLineIndex() {
		return lineIndex;
	}
	
	@Override
	public int compareTo(CodeIndex o) {
		int c = Integer.compare(line, o.line);
		if (c != 0) return c;
		if (lineIndex < 0 || o.lineIndex < 0) return 0;
		return Integer.compare(lineIndex, o.lineIndex);
	}

	@Override
	public int hashCode() {
		return line ^ 31;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CodeIndex other = (CodeIndex) obj;
		if (line != other.line)
			return false;
		if (lineIndex < 0 || other.lineIndex < 0) {
			// special case: wild-card match
			return true;
		}
		if (lineIndex != other.lineIndex)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return line + (lineIndex <= 0 ? "" : "'" + lineIndex);
	}
	
	public static class Manager {
		
		private final Map<Integer, Map<Object, Integer>> lineCounter = new HashMap<>();
		private final Map<Unit, CodeIndex> indices = new HashMap<>();
		
		public CodeIndex get(Unit u) {
			CodeIndex ci = indices.get(u);
			if (ci == null) {
				int line = getLineNumber(u);
				int index = getIndex(u, line);

				ci = new CodeIndex(line, index);
				indices.put(u, ci);
//				System.out.println(u + "    ->    " + ci);
			}
			return ci;
		}
		
		private int getLineNumber(Unit u) {
			Tag t = u.getTag("LineNumberTag");
			LineNumberTag lt = (LineNumberTag) t;
			if (lt != null) return lt.getLineNumber();
			return -1;
		}
		
		private int getIndex(Unit u, int line) {
			Map<Object, Integer> lc = lineCounter.get(line);
			if (lc == null) {
				lc = new HashMap<>();
				lineCounter.put(line, lc);
			}
			Object id = identifier(u);
			int index = lc.getOrDefault(id, 0);
			lc.put(id, index+1);
			return index;
		}
		
		private Object identifier(Unit u) {
			if (u instanceof DefinitionStmt) {
				return ((DefinitionStmt) u).getLeftOp().toString();
			}
			return u.getClass();
		}
	}
}
