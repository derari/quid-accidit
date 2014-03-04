package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class DataDependency {
	
	public static DataDependency variable(String name, int line) {
		return new Variable(name, line);
	}
	
	public static DataDependency argument(int index) {
		return new Argument(index);
	}
	
	public static DataDependency field(DataDependency instance, String name, int line) {
		return new Field(instance, name, line);
	}
	
	public static DataDependency element(DataDependency instance, int index, int line) {
		return new Element(instance, index, line);
	}
	
	public static DataDependency all(Collection<? extends DataDependency> all) {
		List<DataDependency> list = new ArrayList<>();
		for (DataDependency dd: all) {
			if (dd instanceof Constant) {
				continue;
			} if (dd instanceof All) {
				list.addAll(((All) dd).all);
			} else {
				list.add(dd);
			}
		}
		if (list.isEmpty()) return constant();
		if (list.size() == 1) return list.get(0);
		return new All(list);
	}
	
	public static DataDependency conditional(DataDependency condition, DataDependency v1, DataDependency v2) {
		return new Conditional(condition, v1, v2);
	}
	
	private static final ThisValue THIS = new ThisValue();
	private static final Constant CONST = new Constant();
	
	public static DataDependency thisValue() {
		return THIS;
	}
	
	public static DataDependency constant() {
		return CONST;
	}
	
	public static abstract class Direct extends DataDependency {
		
	}
	
	public static class Variable extends Direct {
		
		String var;
		int line;
		
		public Variable(String var, int line) {
			this.var = var;
			this.line = line;
		}
		
		@Override
		public String toString() {
			return var + ":" + line;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + line;
			result = prime * result + ((var == null) ? 0 : var.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Variable other = (Variable) obj;
			if (line != other.line)
				return false;
			if (var == null) {
				if (other.var != null)
					return false;
			} else if (!var.equals(other.var))
				return false;
			return true;
		}
	}
	
	public static class Field extends Direct {
		
		DataDependency instance;
		String field;
		int line;
		public Field(DataDependency instance, String field, int line) {
			super();
			this.instance = instance;
			this.field = field;
			this.line = line;
		}
		
		@Override
		public String toString() {
			return instance + "." + field + ":" + line;
		}
	}
	
	public static class Element extends Direct {
		
		DataDependency instance;
		int index;
		int line;
		public Element(DataDependency instance, int index, int line) {
			super();
			this.instance = instance;
			this.index = index;
			this.line = line;
		}
		
		@Override
		public String toString() {
			return instance + "[" + index + "]:" + line;
		}
	}
	
	public static class Argument extends Direct {
		
		int var;

		public Argument(int var) {
			
			this.var = var;
		}
		
		@Override
		public String toString() {
			return "<-[" + var + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + var;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Argument other = (Argument) obj;
			if (var != other.var)
				return false;
			return true;
		}
	}
	
	public static class ThisValue extends Direct {
		@Override
		public String toString() {
			return "this";
		}
	}
	
	public static class Constant extends Direct {
		@Override
		public String toString() {
			return "const";
		}
	}
	
	public static class All extends Direct {
		
		List<DataDependency> all;

		public All(Collection<? extends DataDependency> all) {
			super();
			this.all = new ArrayList<>(all);
		}
		
		@Override
		public String toString() {
			return Arrays.toString(all.toArray());
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((all == null) ? 0 : all.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			All other = (All) obj;
			if (all == null) {
				if (other.all != null)
					return false;
			} else if (!all.equals(other.all))
				return false;
			return true;
		}
	}

	public static class Conditional extends Direct {
		
		DataDependency condt, opt1, opt2;

		public Conditional(DataDependency condt, DataDependency opt1,
				DataDependency opt2) {
			super();
			this.condt = condt;
			this.opt1 = opt1;
			this.opt2 = opt2;
		}
		
		@Override
		public String toString() {
			return condt + "? " + opt1 + " : " + opt2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((condt == null) ? 0 : condt.hashCode());
			result = prime * result + ((opt1 == null) ? 0 : opt1.hashCode());
			result = prime * result + ((opt2 == null) ? 0 : opt2.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Conditional other = (Conditional) obj;
			if (condt == null) {
				if (other.condt != null)
					return false;
			} else if (!condt.equals(other.condt))
				return false;
			if (opt1 == null) {
				if (other.opt1 != null)
					return false;
			} else if (!opt1.equals(other.opt1))
				return false;
			if (opt2 == null) {
				if (other.opt2 != null)
					return false;
			} else if (!opt2.equals(other.opt2))
				return false;
			return true;
		}
	}
}
