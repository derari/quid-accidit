package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class DataDependency implements Comparable<DataDependency> {
	
	private List<DataDependency> lessThanMe;
	
	@Override
	public int compareTo(DataDependency o) {
		if (equals(o)) return 0;
		int c;
		c = toString().compareTo(o.toString());
		if (c != 0) return c;
		c = getClass().getName().compareTo(o.getClass().getName());
		if (c != 0) return c;
		c = System.identityHashCode(this) - System.identityHashCode(o);
		if (c != 0) return c;
		if (lessThanMe(o)) return 1;
		if (o.lessThanMe(this)) return -1;
		if (lessThanMe == null) lessThanMe = new ArrayList<>();
		System.out.println("!!! " + this + " < " + o);
		lessThanMe.add(o);
		return 1;
	}
	
	private boolean lessThanMe(DataDependency o) {
		if (lessThanMe == null) return false;
		return lessThanMe.contains(o);
	}
	
	protected abstract DataDependency flattenAll();
	
	protected abstract void flattenAll(Set<DataDependency> bag);
	
	protected String nestedString() {
		return toString();
	}
	
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
	
	public static DataDependency all(DataDependency... all) {
		return all(Arrays.asList(all));
	}
	
	public static DataDependency all(Collection<? extends DataDependency> all) {
		Set<DataDependency> set = new TreeSet<>();
		for (DataDependency dd: all) {
			if (dd instanceof Constant) {
				continue;
			} if (dd instanceof All) {
				set.addAll(((All) dd).all);
			} else {
				set.add(dd);
			}
		}
		if (set.isEmpty()) return constant();
		if (set.size() == 1) return set.iterator().next();
		return new All(set);
	}
	
	public static DataDependency choice(DataDependency... choice) {
		return choice(Arrays.asList(choice));
	}
	
	public static DataDependency choice(Collection<? extends DataDependency> choice) {
		Set<DataDependency> set = new TreeSet<>();
		for (DataDependency dd: choice) {
			if (dd instanceof Constant) {
				continue;
			} if (dd instanceof Choice) {
				set.addAll(((Choice) dd).choice);
			} else {
				set.add(dd);
			}
		}
		if (set.isEmpty()) return constant();
		if (set.size() == 1) return set.iterator().next();
		return new Choice(set);
	}
	
	public static DataDependency complex(DataDependency control, DataDependency value) {
		control = control.flattenAll();
		if (value instanceof Complex) {
			Complex v = (Complex) value;
			control = all(control, v.control.flattenAll());
			value = v.value;
		}
		boolean implicitControl = false;
		return new Complex(control, value, implicitControl);
	}
	
//	public static DataDependency conditional(DataDependency condition, DataDependency v1, DataDependency v2) {
//		return new Conditional(condition, v1, v2);
//	}
	
	private static final ThisValue THIS = new ThisValue();
	private static final Constant CONST = new Constant();
	
	public static DataDependency thisValue() {
		return THIS;
	}
	
	public static DataDependency constant() {
		return CONST;
	}
	
	public static abstract class Atomic extends DataDependency {
		
		@Override
		protected DataDependency flattenAll() {
			return this;
		}
		
		protected void flattenAll(Set<DataDependency> bag) {
			bag.add(this);
		}
	}
	
	public static abstract class Composite extends DataDependency {
		
		@Override
		protected DataDependency flattenAll() {
			Set<DataDependency> bag = new TreeSet<>();
			flattenAll(bag);
			return all(bag);
		}
		
		protected abstract void flattenAll(Set<DataDependency> bag);
	}
	
	public static class Variable extends Atomic {
		
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
	
	public static class Field extends Atomic {
		
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
	
	public static class Element extends Atomic {
		
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
	
	public static class Argument extends Atomic {
		
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
	
	public static class ThisValue extends Atomic {
		@Override
		public String toString() {
			return "this";
		}
	}
	
	public static class Constant extends Atomic {
		@Override
		public String toString() {
			return "const";
		}
	}
	
	public static class All extends Composite {
		
		Set<DataDependency> all;

		public All(Collection<? extends DataDependency> all) {
			super();
			this.all = new TreeSet<>(all);
		}
		
		@Override
		protected void flattenAll(Set<DataDependency> bag) {
			for (DataDependency d: all) {
				d.flattenAll(bag);
			}
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
	
	public static class Choice extends Composite {
		
		Set<DataDependency> choice;

		public Choice(Collection<? extends DataDependency> choice) {
			super();
			this.choice = new TreeSet<>(choice);
		}
		
		@Override
		protected void flattenAll(Set<DataDependency> bag) {
			bag.add(this);
		}
		
		@Override
		public String toString() {
			String s = Arrays.toString(choice.toArray());
			return "{" + s.substring(1, s.length()-1) + "}";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((choice == null) ? 0 : choice.hashCode());
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
			if (choice == null) {
				if (other.all != null)
					return false;
			} else if (!choice.equals(other.all))
				return false;
			return true;
		}
	}
	
	public static class Complex extends Composite {
		
		DataDependency control, value;
		boolean implicitControl;

		public Complex(DataDependency control, DataDependency value) {
			super();
			this.control = control;
			this.value = value;
			this.implicitControl = false;
		}
		
		public Complex(DataDependency control, DataDependency value,
				boolean implicitControl) {
			super();
			this.control = control;
			this.value = value;
			this.implicitControl = implicitControl;
		}
		
		@Override
		protected void flattenAll(Set<DataDependency> bag) {
			control.flattenAll(bag);
			value.flattenAll(bag);
		}
		
		@Override
		public String toString() {
			if (implicitControl) {
				return value.toString();
			}
			if (value instanceof Constant) {
				return control + "?";
			}
			return control.nestedString() + " ?-> " + value.nestedString();
		}
		
		@Override
		protected String nestedString() {
			return "(" + toString() + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((control == null) ? 0 : control.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			Complex other = (Complex) obj;
			if (control == null) {
				if (other.control != null)
					return false;
			} else if (!control.equals(other.control))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}

//	public static class ConditionalChoice extends Direct {
//		
//		DataDependency condt, opt1, opt2;
//
//		public ConditionalChoice(DataDependency condt, DataDependency opt1,
//				DataDependency opt2) {
//			super();
//			this.condt = condt;
//			this.opt1 = opt1;
//			this.opt2 = opt2;
//		}
//		
//		@Override
//		public String toString() {
//			return condt + "? " + opt1 + " : " + opt2;
//		}
//
//		@Override
//		public int hashCode() {
//			final int prime = 31;
//			int result = 1;
//			result = prime * result + ((condt == null) ? 0 : condt.hashCode());
//			result = prime * result + ((opt1 == null) ? 0 : opt1.hashCode());
//			result = prime * result + ((opt2 == null) ? 0 : opt2.hashCode());
//			return result;
//		}
//
//		@Override
//		public boolean equals(Object obj) {
//			if (this == obj)
//				return true;
//			if (obj == null)
//				return false;
//			if (getClass() != obj.getClass())
//				return false;
//			ConditionalChoice other = (ConditionalChoice) obj;
//			if (condt == null) {
//				if (other.condt != null)
//					return false;
//			} else if (!condt.equals(other.condt))
//				return false;
//			if (opt1 == null) {
//				if (other.opt1 != null)
//					return false;
//			} else if (!opt1.equals(other.opt1))
//				return false;
//			if (opt2 == null) {
//				if (other.opt2 != null)
//					return false;
//			} else if (!opt2.equals(other.opt2))
//				return false;
//			return true;
//		}
//	}
}
