package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import soot.SootMethod;

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
	
	protected abstract void removeFrom(Collection<DataDependency> controlDependencies);
	
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
	
	public static DataDependency element(DataDependency instance, DataDependency index, int line) {
		return new Element(instance, index, line);
	}
	
	public static Invocation invocation() {
		return new Invocation();
	}
	
	public static InvocationResult invocationResult(SootMethod method, DataDependency self, List<DataDependency> args) {
		String type = method.getDeclaringClass().getName();
		String mName = method.getName();
		String sig = method.getBytecodeSignature();
		int i = sig.indexOf('(');
		sig = sig.substring(i, sig.length()-1); // fix signature format
		
		return new InvocationResult(type, mName, sig, self, args);
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
		
		Set<DataDependency> controlSet = new TreeSet<>();
		if (control instanceof All) {
			controlSet.addAll(((All) control).all);
		} else {
			controlSet.add(control);
		}
		value.removeFrom(controlSet);
		control = all(controlSet);

		if (control instanceof Constant) return value;
		boolean implicitControl = false;
		return new Complex(control, value, implicitControl);
	}
	
	public static DataDependency reach(DataDependency reach, DataDependency value) {
		return new Reach(reach, value);
	}
	
//	public static DataDependency conditional(DataDependency condition, DataDependency v1, DataDependency v2) {
//		return new Conditional(condition, v1, v2);
//	}
	
	private static final ThisValue THIS = new ThisValue();
	private static final Constant CONST = new Constant();
	private static final Catch CATCH = new Catch();
	
	public static DataDependency thisValue() {
		return THIS;
	}
	
	public static DataDependency constant() {
		return CONST;
	}
	
	public static DataDependency caughtException() {
		return CATCH;
	}
	
	public static abstract class Atomic extends DataDependency {
		
		@Override
		protected DataDependency flattenAll() {
			return this;
		}
		
		protected void flattenAll(Set<DataDependency> bag) {
			bag.add(this);
		}
		
		@Override
		protected void removeFrom(Collection<DataDependency> controlDependencies) {
			while (controlDependencies.remove(this));
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
		
		public String getVar() {
			return var;
		}
		
		public int getLine() {
			return line;
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
		
		public DataDependency getInstance() {
			return instance;
		}
		
		public String getField() {
			return field;
		}
		
		public int getLine() {
			return line;
		}
		
		@Override
		public String toString() {
			return instance + "." + field + ":" + line;
		}
	}
	
	public static class Element extends Atomic {
		
		DataDependency instance;
		DataDependency index;
		int line;
		public Element(DataDependency instance, DataDependency index, int line) {
			super();
			this.instance = instance;
			this.index = index;
			this.line = line;
		}
		
		@Override
		public String toString() {
			return instance + "[" + index + "]:" + line;
		}
		
		public DataDependency getInstance() {
			return instance;
		}
		
		public DataDependency getIndex() {
			return index;
		}
		
		public int getLine() {
			return line;
		}
		
		@Override
		protected void removeFrom(Collection<DataDependency> controlDependencies) {
			super.removeFrom(controlDependencies);
			instance.removeFrom(controlDependencies);
			index.removeFrom(controlDependencies);
		}
	}
	
	public static class Argument extends Atomic {
		
		int index;

		public Argument(int var) {
			this.index = var;
		}
		
		public int getIndex() {
			return index;
		}
		
		@Override
		public String toString() {
			return "<-[" + index + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + index;
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
			if (index != other.index)
				return false;
			return true;
		}
	}
	
	public static class Invocation extends Atomic {
		public Invocation() {
		}
		@Override
		public String toString() {
			return "<call>";
		}
	}
	
	public static class InvocationResult extends Atomic {
		String clazz;
		String method;
		String signature;
		DataDependency self;
		List<DataDependency> args;
		
		public InvocationResult(String clazz, String method, String signature, DataDependency self,
				List<DataDependency> args) {
			this.clazz = clazz;
			this.method = method;
			this.signature = signature;
			this.self = self;
			this.args = new ArrayList<>(args);
//			if (self != null) this.args.add(self);
//			this.args.addAll(args);
		}
		
		public DataDependency getSelf() {
			return self;
		}
		
		public List<DataDependency> getArgs() {
			return args;
		}
		
		public DataDependency getArg(int i) {
			return args.get(i);
		}
		
		public int getArgC() {
			return args.size();
		}
		
		public String getMethodKey() {
			return clazz + "#" + method + signature;
		}
		
		public String getType() {
			return clazz;
		}
		
		public String getMethod() {
			return method;
		}
		
		public String getSignature() {
			return signature;
		}
		
		@Override
		public String toString() {
			String s = getMethodKey();
			if (self != null) s += "<" + self + ">";
			s += "(";
			if (args.isEmpty()) return s + ")";
			for (DataDependency a: args) {
				s += a + ", ";
			}
			return s.substring(0, s.length()-2) + ")";
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
	
	public static class Catch extends Atomic {
		@Override
		public String toString() {
			return "catch";
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
		protected void removeFrom(Collection<DataDependency> controlDependencies) {
			for (DataDependency d: all) {
				d.removeFrom(controlDependencies);
			}
		}
		
		public Set<DataDependency> getAll() {
			return all;
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
		
		SortedSet<DataDependency> choice;

		public Choice(Collection<? extends DataDependency> choice) {
			super();
			this.choice = new TreeSet<>(choice);
		}
		
		@Override
		protected void flattenAll(Set<DataDependency> bag) {
			for (DataDependency d: choice) {
				d.flattenAll(bag);
			}
//			Set<DataDependency> flat = new TreeSet<>();
//			for (DataDependency d: choice) {
//				Set<DataDependency> subBag = new TreeSet<>();
//				d.flattenAll(subBag);
//				for (DataDependency d2: bag) {
//					d2.removeFrom(subBag);
//				}
//				if (!subBag.isEmpty()) {
//					flat.add(all(subBag));
//				}
////				if (bag.contains(d)) {
////					continue;
////				}
////				if (d instanceof All && bag.containsAll(((All) d).all)) {
////					continue;
////				}
////				flat.add(d);
//			}
////			if (flat.size() == 1) {
////				bag.addAll(flat);
////			} else 
//			if (!flat.isEmpty()) {
//				bag.add(new ZChoice(flat));
//			}
		}
		
		@Override
		protected void removeFrom(Collection<DataDependency> controlDependencies) {
			for (DataDependency d: choice) {
				d.removeFrom(controlDependencies);
			}
			while (controlDependencies.remove(this));
		}
		
		public SortedSet<DataDependency> getChoice() {
			return choice;
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
			Choice other = (Choice) obj;
			if (choice == null) {
				if (other.choice != null)
					return false;
			} else if (!choice.equals(other.choice))
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
		protected void removeFrom(Collection<DataDependency> controlDependencies) {
			control.removeFrom(controlDependencies);
			value.removeFrom(controlDependencies);
			while (controlDependencies.remove(this));
		}
		
		public DataDependency getControl() {
			return control;
		}
		
		public DataDependency getValue() {
			return value;
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
	
	public static class Reach extends Composite {
		
		DataDependency reach, value;
		
		public Reach(DataDependency reach, DataDependency value) {
			super();
			this.reach = reach;
			this.value = value;
		}

		public DataDependency getReach() {
			return reach;
		}
		
		public DataDependency getValue() {
			return value;
		}

		@Override
		protected void flattenAll(Set<DataDependency> bag) {
			reach.flattenAll(bag);
			value.flattenAll(bag);
		}
		
		@Override
		protected void removeFrom(Collection<DataDependency> controlDependencies) {
			reach.removeFrom(controlDependencies);
			value.removeFrom(controlDependencies);
			while (controlDependencies.remove(this));
		}
		
		@Override
		public String toString() {
			return value.toString();
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
