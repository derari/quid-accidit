package de.hpi.accidit.eclipse.slice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Objects;

import soot.SootMethod;

public abstract class CodeDependency implements Comparable<CodeDependency> {
	
	private List<CodeDependency> lessThanMe;
	
	@Override
	public int compareTo(CodeDependency o) {
		if (equals(o)) return 0;
		int c;
		c = toString().compareTo(o.toString());
		if (c != 0) return c;
		c = getClass().getName().compareTo(o.getClass().getName());
		if (c != 0) return c;
		c = System.identityHashCode(this) - System.identityHashCode(o);
		if (c != 0) return c;
		if (greaterThan(o)) return 1;
		if (o.greaterThan(this)) return -1;
		if (lessThanMe == null) lessThanMe = new ArrayList<>();
		lessThanMe.add(o);
		return 1;
	}
	
	private boolean greaterThan(CodeDependency o) {
		if (lessThanMe == null) return false;
		if (lessThanMe.contains(o)) return true;
		for (CodeDependency lt: lessThanMe) {
			if (lt.greaterThan(o)) {
				lessThanMe.add(o);
				return true;
			}
		}
		return false;
	}
	
	public static ValueDependency variable(String name, CodeIndex ci) {
		return new Variable(name, ci);
	}
	
	public static ValueDependency argument(int index) {
		return new Argument(index);
	}
	
	public static ValueDependency field(CVDependency instance, String name, CodeIndex ci) {
		return new Field(instance, name, ci);
	}
	
	public static ValueDependency element(CVDependency instance, CVDependency index, CodeIndex ci) {
		return new Element(instance, index, ci);
	}
	
	public static Invocation invocation() {
		return new Invocation();
	}
	
	public static InvocationResult invocationResult(SootMethod method, CVDependency self, List<CVDependency> args, CodeIndex ci) {
		String type = method.getDeclaringClass().getName();
		String mName = method.getName();
		String sig = method.getBytecodeSignature();
		int i = sig.indexOf('(');
		sig = sig.substring(i, sig.length()-1); // fix signature format
		
		return new InvocationResult(type, mName, sig, self, args, ci);
	}
	
	public static CVDependency allCV(Collection<? extends CVDependency> all) {
		Set<CVDependency> set = new TreeSet<>();
		for (CVDependency dd: all) {
			if (dd instanceof Constant) {
				continue;
			} if (dd instanceof All) {
				set.addAll(((All) dd).all);
			} if (dd instanceof AllValues) {
				set.addAll(((AllValues) dd).all);
			} else {
				set.add(dd);
			}
		}
		if (set.isEmpty()) return constant();
		if (set.size() == 1) {
			return set.iterator().next();
		}
		return new All(set);
	}
	
	public static ValueDependency all(ValueDependency... all) {
		return all(Arrays.asList(all));
	}
	
	public static ValueDependency all(Collection<? extends ValueDependency> all) {
		Set<ValueDependency> set = new TreeSet<>();
		for (ValueDependency dd: all) {
			if (dd instanceof Constant) {
				continue;
			} if (dd instanceof AllValues) {
				set.addAll(((AllValues) dd).all);
			} else {
				set.add(dd);
			}
		}
		if (set.isEmpty()) return constant();
		if (set.size() == 1) {
			CVDependency cd = set.iterator().next();
			if (cd instanceof ValueDependency) return (ValueDependency) cd;
		}
		return new AllValues(set);
	}
	
	public static ValueDependency allFlat(CVDependency... all) {
		return allFlat(Arrays.asList(all));
	}
	
	public static ValueDependency allFlat(Collection<? extends CVDependency> all) {
		List<ValueDependency> flat = new ArrayList<>();
		for (CVDependency cd: all) {
			flat.add(cd.flat());
		}
		return all(flat);
	}
	
	public static AnyValue anyOf(CVDependency... choice) {
		return anyOf(Arrays.asList(choice));
	}
	
	public static AnyValue anyOf(Collection<? extends CVDependency> choice) {
		Set<CVDependency> set = new TreeSet<>();
		for (CVDependency cd: choice) {
			if (cd instanceof Constant) {
				continue;
			} else {
				set.add(cd);
			}
		}
//		if (set.isEmpty()) return constant();
//		if (set.size() == 1) return set.iterator().next();
		return new AnyValue(set);
	}
//	
//	public static CodeDependency complex(CodeDependency control, CodeDependency value) {
//		control = control.flattenAll();
//		if (value instanceof Complex) {
//			Complex v = (Complex) value;
//			control = all(control, v.control.flattenAll());
//			value = v.value;
//		}
//		
//		Set<CodeDependency> controlSet = new TreeSet<>();
//		if (control instanceof All) {
//			controlSet.addAll(((All) control).all);
//		} else {
//			controlSet.add(control);
//		}
//		value.removeFrom(controlSet);
//		control = all(controlSet);
//
//		if (control instanceof Constant) return value;
//		boolean implicitControl = false;
//		return new Complex(control, value, implicitControl);
//	}
//	
//	public static CodeDependency reach(CodeDependency reach, CodeDependency value) {
//		return new Reach(reach, value);
//	}
	
//	public static DataDependency conditional(DataDependency condition, DataDependency v1, DataDependency v2) {
//		return new Conditional(condition, v1, v2);
//	}
	
	private static final ThisValue THIS = new ThisValue();
	private static final Constant CONST = new Constant();
	private static final Catch CATCH = new Catch();
	
	public static ValueDependency thisValue() {
		return THIS;
	}
	
	public static ValueDependency constant() {
		return CONST;
	}
	public static ValueDependency noDependency() {
		return CONST;
	}
	
	public static ValueDependency caughtException() {
		return CATCH;
	}
	
	/**
	 * A plain dependency.
	 */
	public static abstract class ValueDependency extends CVDependency {
		
		public ValueDependency() {
		}

		@Override
		public ValueDependency getValue() {
			return this;
		}
		
		@Override
		public final ValueDependency getControl() {
			return constant();
		}
	}
	
	/**
	 * A value dependency, annotated with a control dependency.
	 */
	public static abstract class CVDependency extends CodeDependency {
		
		public abstract ValueDependency getValue();
		
		public abstract ValueDependency getControl();
		
		public TriDependency reachVia(CVDependency reach) {
			return new TriDependency(this, reach.flat());
		}
		
		private ValueDependency flat() {
			return all(getValue(), getControl());
		}
	}
	
	/**
	 * Distinguishes between value, reach, and control dependencies.
	 */
	public static class TriDependency extends CodeDependency {

		private CVDependency value;
		private ValueDependency reach;
		
		public TriDependency(CVDependency value, ValueDependency reach) {
			this.value = value;
			this.reach = reach;
		}

		public ValueDependency getValue() {
			return value.getValue();
		}
		
		public CVDependency getCValue() {
			return value;
		}
		
		public ValueDependency getReach() {
			return reach;
		}
		
		public ValueDependency getControl() {
			return value.getControl();
		}

		@Override
		public String toString() {
			return reach + " -> " + value;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((reach == null) ? 0 : reach.hashCode());
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
			TriDependency other = (TriDependency) obj;
			if (reach == null) {
				if (other.reach != null)
					return false;
			} else if (!reach.equals(other.reach))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}
	
	/** Dependency to a single value. */
	public static abstract class Atomic extends ValueDependency {
	}
	
	public static class Variable extends Atomic {
		String var;
		CodeIndex ci;
		
		public Variable(String var, CodeIndex ci) {
			super();
			this.var = var;
			this.ci = ci;
		}

		public String getVar() {
			return var;
		}
		
		public CodeIndex getCodeIndex() {
			return ci;
		}
		
		@Override
		public String toString() {
			return var + ":" + ci;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ci.hashCode();
			result = prime * result + var.hashCode();
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
			if (!Objects.equal(ci, other.ci))
				return false;
			if (!Objects.equal(var, other.var))
				return false;
			return true;
		}
	}
	
	public static class Field extends Atomic {
		
		CVDependency instance;
		String field;
		CodeIndex ci;
		
		public Field(CVDependency instance, String field, CodeIndex ci) {
			super();
			this.instance = instance;
			this.field = field;
			this.ci = ci;
		}
		
		public CVDependency getInstance() {
			return instance;
		}
		
		public String getField() {
			return field;
		}
		
		public CodeIndex getCodeIndex() {
			return ci;
		}
		
		@Override
		public String toString() {
			return instance + "." + field + ":" + ci;
		}
	}
	
	public static class Element extends Atomic {
		
		CVDependency instance;
		CVDependency index;
		CodeIndex ci;
		
		public Element(CVDependency instance, CVDependency index, CodeIndex ci) {
			super();
			this.instance = instance;
			this.index = index;
			this.ci = ci;
		}
		
		@Override
		public String toString() {
			return instance + "[" + index + "]:" + ci;
		}
		
		public CVDependency getInstance() {
			return instance;
		}
		
		public CVDependency getIndex() {
			return index;
		}
		
		public CodeIndex getCodeIndex() {
			return ci;
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
		CVDependency self;
		List<CVDependency> args;
		CodeIndex ci;
		
		public InvocationResult(String clazz, String method, String signature, CVDependency self,
				List<CVDependency> args, CodeIndex ci) {
			this.clazz = clazz;
			this.method = method;
			this.signature = signature;
			this.self = self;
			this.args = new ArrayList<>(args);
			this.ci = ci;
//			if (self != null) this.args.add(self);
//			this.args.addAll(args);
		}
		
		public CVDependency getSelf() {
			return self;
		}
		
		public List<CVDependency> getArgs() {
			return args;
		}
		
		public CodeDependency getArg(int i) {
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
			String s = ci + ":" + getMethodKey();
			if (self != null) s += "<" + self + ">";
			s += "(";
			if (args.isEmpty()) return s + ")";
			for (CodeDependency a: args) {
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
	
	public static class All extends CVDependency {
		private final Set<CVDependency> all;
		private ValueDependency value, control;

		public All(Collection<? extends CVDependency> all) {
			this.all = new TreeSet<>(all);
		}
		
		public Set<CVDependency> getAll() {
			return all;
		}
		
		@Override
		public ValueDependency getValue() {
			if (value == null) {
				List<ValueDependency> values = new ArrayList<>();
				for (CVDependency cv: all) {
					values.add(cv.getValue());
				}
				value = all(values);
			}
			return value;
		}
		
		@Override
		public ValueDependency getControl() {
			if (control == null) {
				List<ValueDependency> controls = new ArrayList<>();
				for (CVDependency cv: all) {
					controls.add(cv.getControl());
				}
				control = all(controls);
			}
			return control;
		}
		@Override
		public String toString() {
			return Arrays.toString(all.toArray());
		}

		@Override
		public int hashCode() {
			return getValue().hashCode() ^ getControl().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AllValues other = (AllValues) obj;
			return getValue().equals(other.getValue())
					&& getControl().equals(other.getControl());
		}
	}
	
	public static class AllValues extends ValueDependency {
		
		private final Set<ValueDependency> all;

		public AllValues(Collection<? extends ValueDependency> all) {
			this.all = new TreeSet<>(all);
		}
		
		public Set<ValueDependency> getAll() {
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
			AllValues other = (AllValues) obj;
			if (all == null) {
				if (other.all != null)
					return false;
			} else if (!all.equals(other.all))
				return false;
			return true;
		}
	}
	
	public static class AnyValue extends ValueDependency {
		
		SortedSet<CVDependency> options;
		ValueDependency control = constant(); //todo: delete

		public AnyValue(Collection<? extends CVDependency> choice) {
			this.options = new TreeSet<>(choice);
		}
		
		public SortedSet<CVDependency> getChoice() {
			return options;
		}
		
		@Override
		public String toString() {
			String s = Arrays.toString(options.toArray());
			return "{" + s.substring(1, s.length()-1) + "}";
		}
		
		public CVDependency withControl(ValueDependency control) {
			if (options.isEmpty()) return constant();
			if (options.size() == 1) return options.first();
			List<CVDependency> allOptions = new ArrayList<>();
			List<ValueDependency> allControls = new ArrayList<>();
			for (CVDependency cd: options) {
				if (cd instanceof Choice) {
					Choice c = (Choice) cd;
					allControls.add(c.getControl());
					cd = c.getValue();
				}
				if (cd instanceof AnyValue) {
					allOptions.addAll(((AnyValue) cd).options);
				} else {
					allOptions.add(cd);
				}
			}
			allControls.add(control);
			return new Choice(anyOf(allOptions), all(allControls));
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((options == null) ? 0 : options.hashCode());
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
			AnyValue other = (AnyValue) obj;
			if (options == null) {
				if (other.options != null)
					return false;
			} else if (!options.equals(other.options))
				return false;
			return true;
		}
	}
	
	public static class Choice extends CVDependency {
		private AnyValue value;
		private ValueDependency control;

		public Choice(AnyValue value, ValueDependency control) {
			super();
			this.value = value;
			this.control = control;
		}

		@Override
		public AnyValue getValue() {
			return value;
		}

		@Override
		public ValueDependency getControl() {
			return control;
		}
		
		@Override
		public String toString() {
			return control + "?" + value;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((control == null) ? 0 : control.hashCode());
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
			Choice other = (Choice) obj;
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
}
