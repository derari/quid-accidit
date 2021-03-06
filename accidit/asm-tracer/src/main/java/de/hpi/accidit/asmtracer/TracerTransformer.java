package de.hpi.accidit.asmtracer;

import de.hpi.accidit.model.*;
import de.hpi.accidit.trace.Tracer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

@SuppressWarnings("CallToThreadDumpStack")
public class TracerTransformer implements ClassFileTransformer {
    
    public static boolean fullTracingEnabled = true;
    
    private static class DoNotInstrumentException extends RuntimeException { }
    
    private static final String AtTraced = "Lde/hpi/accidit/asmtracer/Traced;";
    
    private static final String[] excludes = {
        // tracing infrastructure
        "de/hpi/accidit/asm",
        "de/hpi/accidit/model",
        "de/hpi/accidit/trace",
        "de/hpi/accidit/out",
        "org/objectweb/asm",

        // excluded for technical reasons
        "$",
        "java/lang/Class",
        "java/lang/Enum",
        "java/lang/ref",
        "java/util/concurrent",
        "java/util/security",
        "java/security",
        "sun",
        "org/h2",
        "junit/framework/TestCase",
        "java/util/IdentityHashMap$IdentityHashMapIterator",
//        "org/camunda/bpm/engine/impl/juel/Parser",
        
        // excluded for performance reasons
//        "java.io",
        
        // excluded libraries
//        "com/sun",
        "org/eclipse",
        "org/drools/rule/GroupElement",
        "org/apache/maven/surefire/util",
    };
    
    private static final String[] no_details = {
        "java",
        "jdk",
        "sun",
        "com/sun",
        "org/mvel2/asm",
        "org/apache/ibatis",
    };
    
    private static final String[] do_details = {
        "java/lang/Boolean",
        "java/lang/Byte",
        "java/lang/Character",
        "java/lang/Double",
        "java/lang/Float",
        "java/lang/Integer",
        "java/lang/Long",
        "java/lang/Short",
        "java/lang/String",
        "java/util",
    };
    
    private static int classCounter = 0;
    private static final Map<Object, Map<String, Class>> knownClasses = new WeakHashMap<>();
    private static final Object NO_CL = new Object();
    
    private static synchronized Class getKnownClass(ClassLoader cl, String clazz) {
        Class c = null;
        while (c == null) {
            Object key = cl != null ? cl : NO_CL;
            Map<String, Class> map = knownClasses.get(key);
            if (map != null) {
                c = map.get(clazz);
            }
            if (cl == null) break;
            cl = cl.getParent();
        }
        if (c == null) {
            try {
                //System.out.println("forname> " + clazz + " " + cl);
                try {
                    c = Class.forName(clazz, false, cl);
                } catch (ClassNotFoundException e) {
                    //System.out.println("unknown class: " + clazz);
                    return null;
                }
                putKnownClass(clazz, c);
            } catch (ClassCircularityError e) {
                return null;
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            }
        }
        return c;
    }
    
    private static synchronized void putKnownClass(String clazzName, Class clazz) {
        if (clazz == null) return;
        ClassLoader cl = clazz.getClassLoader();
        Object key = cl != null ? cl : NO_CL;
        Map<String, Class> map = knownClasses.get(key);
        if (map == null) {
            map = new HashMap<>();
            knownClasses.put(key, map);
        }
        map.put(clazzName, clazz);
    }
    
    private final List<Class> circulars = new ArrayList<>();

    public TracerTransformer() {
    }
    
    public static TraceFilter TRACE_FILTER = new MainMethodTraceFilter();

    @Override
    public byte[] transform(ClassLoader loader, String className, 
                        Class<?> classBeingRedefined, 
                        ProtectionDomain protectionDomain, 
                        byte[] classfileBuffer) 
                            throws IllegalClassFormatException {
        synchronized (Tracer.class) {
            boolean t = Tracer.pauseTrace();
            try {
                if (className != null) {
                    putKnownClass(className.replace('.', '/'), classBeingRedefined);
                    if (className.contains("String")) {
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        System.out.println(className);
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    }
                }
                PreMain.Init.TO_BE_REDEFINED.remove(classBeingRedefined);
                if (!circulars.isEmpty()) {
                    try {
                        Class[] moreClasses = circulars.toArray(new Class[0]);
                        circulars.clear();
                        PreMain.inst.retransformClasses(moreClasses);
                    } catch (UnmodifiableClassException ex) {
                        ex.printStackTrace();
                    }
                }
                return transformUntraced(className, classfileBuffer, loader);
            } catch (ClassCircularityError e) {
                circulars.add(classBeingRedefined);
                return classfileBuffer;
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                Tracer.resumeTrace(t);
            }
        }
    }

    private byte[] transformUntraced(String className, byte[] classfileBuffer, ClassLoader loader) {
        if (className != null) {
            for (String e: excludes) {
                if (className.startsWith(e))
                    return classfileBuffer;
            }
        }
        if (className == null) {
            return classfileBuffer;
        }
//        if (className.equals("org/netbeans/mdr/storagemodel/StorableObject") || 
//                className.equals("org/argouml/configuration/ConfigurationFactory")) {
//            System.out.println(">> " + className + "     " + loader);
//            return classfileBuffer;
//        }
        if (fullTracingEnabled) {
            if (++classCounter % 1000 == 0) System.out.println(" >> traced classes: " + classCounter);
            System.out.print(">> " + className + "     " + loader);
//            if (className.equals("java/lang/invoke/MemberName$Factory")) {
//                System.out.println("--");
//            }
        }
        try {
            return transform(className, classfileBuffer, loader);
        } finally {
            System.out.println(" ok");
        }
    }

    public static byte[] transform(String className, byte[] classfileBuffer, ClassLoader cl) {
        try {
            return transform(className, classfileBuffer, Tracer.model, cl);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    public static byte[] transform(String className, byte[] classfile, Model model, ClassLoader cl) throws Exception {
        try {
            if (cl instanceof NoTraceClassLoader) return classfile;
            ClassReader cr = new ClassReader(classfile);
//            int flags = (className.contains("Parser") || className.contains("XSDHandler")) 
//                    ? 0 : ClassWriter.COMPUTE_FRAMES;
            int flags = 0;
            ClassWriter cw = new MyClassWriter(cl, flags);
            CheckClassAdapter cca = new CheckClassAdapter(cw, false);
            ClassVisitor transform = new MyClassVisitor(TRACE_FILTER, cca, model, cl);
            cr.accept(transform, 0);
//            TracerTransformer2.tranform(cr, cw);
            return cw.toByteArray();
        } catch (DoNotInstrumentException e) {
            return classfile;
        } catch (Exception e) {
            throw e;
        }
    }
    
    static class MyClassVisitor extends ClassVisitor implements Opcodes {
        
        final TraceFilter traceFilter;
        Object filterData;
        boolean isAlreadyTraced = false;
        boolean isTracedFlagSet = false;
        boolean noDetails;
        boolean hasSource = false;
        
        TypeDescriptor type;
        final ClassLoader cl;
        final Model model;
        String superName;
        String[] interfaces;
        
        
        public MyClassVisitor(TraceFilter traceFilter, ClassVisitor cv, Model model, ClassLoader cl) {
            super(ASM4, cv);
            this.traceFilter = traceFilter;
            this.model = model;
            this.cl = cl;
        }

        @Override
        public void visitSource(String source, String debug) {
            super.visitSource(source, debug);
            type.setSource(source);
            hasSource = true;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            filterData = traceFilter.visitClass(name, superName);
            type = model.getType(name.replace('/', '.'), cl);
            this.superName = superName;
            this.interfaces = interfaces;
            for (String s: no_details) {
                if (name.startsWith(s)) {
                    boolean exclude = true;
                    for (String s2: do_details) {
                       if (name.startsWith(s2)) {
                           exclude = false;
                       }
                    }
                    if (exclude) {
                        noDetails = true;
                        break;
                    }
                }
            }
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(String string, boolean bln) {
            if (string.equals(AtTraced)) {
                isAlreadyTraced = true;
                throw new DoNotInstrumentException();
            }
//            System.out.println("@ " + string + " " + bln);
            return super.visitAnnotation(string, bln);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor sup = super.visitMethod(access, name, desc, signature, exceptions);
            if (isAlreadyTraced) return sup;
            if (!isTracedFlagSet && fullTracingEnabled) {
                isTracedFlagSet = true;
                super.visitAnnotation(AtTraced, false).visitEnd();
                
                if (!type.isInitialized()) {
                    if (superName != null)
                        type.addSuper(superName.replace('/', '.'));
                    for (String iface: interfaces)
                        type.addSuper(iface.replace('/', '.'));
                    type.initCompleted();
                }
            }
            if ((access & (ACC_NATIVE | ACC_BRIDGE | ACC_ABSTRACT | ACC_INTERFACE)) != 0) {
                return sup;
            }
            if (name.equals("finalize") || name.equals("<clinit>")) { //  || name.equals("uncaughtException")
                return sup;
            }
//            if (noDetails && (access & ACC_PUBLIC) == 0) {
//                return sup;
//            }
            if (!hasSource) {
//                return sup;
            }
            return new MyMethodVisitor(traceFilter, filterData, access, name, desc, sup, type, model, cl, noDetails, exceptions);
        }
        
    }
    
    static class MyMethodVisitor extends MethodVisitor implements Opcodes {
        
        private static boolean DEBUG = false;
        
        private static final String TRACER = "de/hpi/accidit/trace/Tracer";
        private static final String LINE = "line";
        private static final String LINE_DESC = "(I)V";
        private static final String CALL = "call";
        private static final String CALL_DESC = "(I)V";
        private static final String ENTER = "enter";
        private static final String ENTER_DESC = "(ILjava/lang/Object;Z)V";
        private static final String RETURN_ = "return";
        private static final String RETURN_DESC_2 = "II)V";
        private static final String STORE_ = "store";
        private static final String STORE_DESC_2 = "III)V";
        private static final String ARG_ = "arg";
        private static final String ARG_DESC_2 = "I)V";
        private static final String PUT_ = "put";
        private static final String PUT_DESC_1 = "(Ljava/lang/Object;";
        private static final String PUT_DESC_2 = "II)V";
        private static final String GET_ = "get";
        private static final String GET_DESC_1 = "(Ljava/lang/Object;";
        private static final String GET_DESC_2 = "II)V";
        private static final String ASTORE_ = "aStore";
        private static final String ASTORE_DESC_1 = "(Ljava/lang/Object;I";
        private static final String ASTORE_DESC_2 = "I)V";
        private static final String ALOAD_ = "aLoad";
        private static final String ALOAD_DESC_1 = "(Ljava/lang/Object;I";
        private static final String ALOAD_DESC_2 = "I)V";
        private static final String THROW = "thrown";
        private static final String THROW_DESC = "(Ljava/lang/Object;I)V";
        private static final String CATCH = "caught";
        private static final String CATCH_DESC = "(Ljava/lang/Object;I)V";

        
        private static final String BEGIN = "begin";
        private static final String BEGIN_DESC = "()V";
        private static final String END = "end";
        private static final String END_DESC = "()V";
        
        private final TraceFilter traceFilter;
        private final Object traceFilterData;
        private final ClassLoader cl;
        private final Model model;
        private final String name;
        private final String descriptor;
        private final boolean isStatic;
        private final boolean isInit;
        private final boolean traceDetails;
        private String[] exceptions;
        private boolean isTraceEntry;
        private boolean tracingEnabled = true;
        private boolean forceTracing = false;
        private final TypeDescriptor type;
        private final MethodDescriptor me;
        private boolean methodLineSet = false;
        private int lastLine = -1;
        private int lastTracedLine = -2;
        private int lastOffset = 0;
        private boolean visitMethodInsnGuard = false;
        
        private final Set<Label> exHandlers = new HashSet<>();
        private final List<String> argTypes = new ArrayList<>();
        
        public MyMethodVisitor(TraceFilter traceFilter, Object classFilterData, int access, String name, String desc, MethodVisitor mv, TypeDescriptor type, Model model, ClassLoader cl, boolean noDetails, String[] exceptions) {
            super(ASM4, mv);
//            DEBUG = type.getName().endsWith("/String;") || type.getName().endsWith(".String");
            this.traceFilter = traceFilter;
            this.traceFilterData = traceFilter.visitMethod(name, classFilterData);
            this.name = name;
            this.descriptor = desc;
            this.model = model;
            this.cl = cl;
            this.traceDetails = !noDetails;
            isTraceEntry = traceFilter.isTraceEntry(name, traceFilterData);
            //System.out.println("- " + name + " " + (test ? "t" : ""));
            this.type = type;
            this.me = type.getMethod(name, desc);
            if (DEBUG) System.out.println("\n" + type + " " + name + desc);
            isStatic = (access & ACC_STATIC) != 0;
            isInit = name.equals("<init>");
            this.exceptions = exceptions;
            this.forceTracing = type.getName().startsWith("java");
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            isTraceEntry |= traceFilter.isTraceEntryAt(desc, visible, isTraceEntry, traceFilterData);
            if (desc.equals(AtTraced)) {
                tracingEnabled = false;
            }
            return super.visitAnnotation(desc, visible);
        }
        
        @Override
        public void visitCode() {
            if (isTraceEntry) {
                if (tracingEnabled) {
                    super.visitAnnotation(AtTraced, false).visitEnd();
                }
            } else {
                tracingEnabled = fullTracingEnabled || forceTracing;
            }
            super.visitCode();
            if (!tracingEnabled) return;
            if (isTraceEntry) {
                System.out.println("- " + name);
                superVisitMethodInsn(INVOKESTATIC, TRACER, BEGIN, BEGIN_DESC);
            }
            super.visitLdcInsn(me.getCodeId());
            if (isStatic) {
                super.visitInsn(ACONST_NULL);
                super.visitLdcInsn(0);
            } else {
                super.visitVarInsn(ALOAD, 0);
                super.visitLdcInsn(isInit ? 1 : 0);
            }
            superVisitMethodInsn(INVOKESTATIC, TRACER, ENTER, ENTER_DESC);
            traceArgs();
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            if (DEBUG) System.out.println(index + " " + desc + "/" + signature + " " + name + " " + start.getOffset() + "-" + end.getOffset());
            super.visitLocalVariable(name, desc, signature, start, end, index);
            if (me.variablesAreInitialized()) return;
            String varType = org.objectweb.asm.Type.getType(desc).getClassName();
            VarDescriptor vdesc = me.addVariable(index, start.getOffset(), name, varType);
            if (index < argTypes.size()) argTypes.set(index, null);
            if (DEBUG) System.out.println(" " + vdesc.getId());
//            if (DEBUG) System.out.println(vdesc);
        }

        @Override
        public void visitLabel(Label label) {
            super.visitLabel(label);
            if (!tracingEnabled) return;
            if (DEBUG) System.out.println("L " + label.getOffset());
            if (exHandlers.remove(label)) {
                traceCatch();
            }
            lastOffset = label.getOffset();
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (tracingEnabled) {
                if (DEBUG) System.out.println("# " + line);
                if (!methodLineSet) {
                    methodLineSet = true;
                    me.setLine(line);
                }
                lastLine = line;
            }
            super.visitLineNumber(line, start);
        }
        
        private void ensureLineNumberIsTraced() {
            if (lastTracedLine != lastLine) {
                super.visitLdcInsn(lastLine);
                lastTracedLine = lastLine;
                superVisitMethodInsn(INVOKESTATIC, TRACER, LINE, LINE_DESC);
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            visitMethodInsnGuard = true;
            try {
                if (!tracingEnabled) {
                    superVisitMethodInsn(opcode, owner, name, desc, itf);
                    return;
                }
                if (owner.equals(TRACER)) {
                    throw new DoNotInstrumentException();
                }
                if (traceDetails) {
                    ensureLineNumberIsTraced();

                    MethodDescriptor md = model.getType(owner.replace('/', '.'), cl).getMethod(name, desc);
                    super.visitLdcInsn(md.getCodeId());
                    super.visitMethodInsn(INVOKESTATIC, TRACER, CALL, CALL_DESC, false);
                }

                itf = itf && (opcode == Opcodes.INVOKEINTERFACE);
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            } finally {
                visitMethodInsnGuard = false;
            }
        }
        
        public void superVisitMethodInsn(int opcode, String owner, String name, String desc) {
            boolean itf = opcode == Opcodes.INVOKEINTERFACE;
            superVisitMethodInsn(opcode, owner, name, desc, itf);
        }
        
        public void superVisitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            boolean g = visitMethodInsnGuard;
            visitMethodInsnGuard = true;
            try {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            } finally {
                visitMethodInsnGuard = g;
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (visitMethodInsnGuard) {
                super.visitMethodInsn(opcode, owner, name, desc);
            } else {
                boolean itf = opcode == Opcodes.INVOKEINTERFACE;
                visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            super.visitIincInsn(var, increment);
            if (!tracingEnabled) return;
            traceInc(ArgumentType.INT, var);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            
            if (tracingEnabled && !exHandlers.isEmpty()) {
                System.out.println(name + " " + exHandlers);
                System.out.println("\n------------------");
            }
            
            if (me.variablesAreInitialized()) return;
            
            for (int i = 0; i < argTypes.size(); i++) {
                String argType = argTypes.get(i);
                if (argType != null) {
                    if (DEBUG) System.out.println(i + " " + argType + " ??");
                    String argTypeName;
                    String varName;
                    if (i == 0 && !isStatic) {
                        argTypeName = me.getOwner().getName();
                        varName = "this";
                    } else {
                        argTypeName = TypeDescriptor.descriptorToName(argType);
                        varName = "$"+i;
                    }
                    me.addVariable(i, 0, varName, argTypeName);
                }
                VarDescriptor var =  me.getVariable(i, 0);
                if (var != null) var.setArgument(true);
            }
            me.variablesCompleted();
        }

        private void traceReturn(ArgumentType type) {
            if (DEBUG) System.out.println("R " + type);
            String desc;
            String method = RETURN_ + type.getKey();
            if (type == ArgumentType.VOID) {
                desc = "(" + RETURN_DESC_2;
            } else {
                super.visitInsn(type.DUPw());
                desc = "(" + type.getDescriptor() + RETURN_DESC_2;
            }
            super.visitLdcInsn(me.getCodeId());
            super.visitLdcInsn(lastLine);
            superVisitMethodInsn(INVOKESTATIC, TRACER, method, desc);

            if (isTraceEntry) {
                superVisitMethodInsn(INVOKESTATIC, TRACER, END, END_DESC);
            }
        }

        private void traceStore(ArgumentType type, int var) {
            if (!traceDetails) return;
            if (DEBUG) System.out.println("S " + type + " " + var);
            String desc;
            String method = STORE_ + type.getKey();
            super.visitInsn(type.DUPw());
            desc = "(" + type.getDescriptor() + STORE_DESC_2;
            super.visitLdcInsn(var);
            super.visitLdcInsn(lastLine);
            super.visitLdcInsn(lastOffset);
            superVisitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }

        private void traceInc(ArgumentType type, int var) {
            if (!traceDetails) return;
            if (DEBUG) System.out.println("S " + type + " " + var);
            String desc;
            String method = STORE_ + type.getKey();
            super.visitVarInsn(type.LOAD(), var);
            desc = "(" + type.getDescriptor() + STORE_DESC_2;
            super.visitLdcInsn(var);
            super.visitLdcInsn(lastLine);
            super.visitLdcInsn(lastOffset);
            superVisitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }

        private void traceArg(ArgumentType type, int var) {
            if (DEBUG) System.out.println("A " + type + " " + var);
//            super.visitLdcInsn(var);
//            super.visitMethodInsn(INVOKESTATIC, TRACER, "dummyI", "(I)V");
            String desc;
            String method = ARG_ + type.getKey();
            super.visitVarInsn(type.LOAD(), var);
            desc = "(" + type.getDescriptor() + ARG_DESC_2;
            super.visitLdcInsn(var);
            superVisitMethodInsn(INVOKESTATIC, TRACER, method, desc);          
        }

        private void tracePutStatic(FieldDescriptor f, ArgumentType type) {
            if (DEBUG) System.out.println("F " + f + " static");
            super.visitInsn(type.DUPw());
            String method = PUT_ + type.getKey();
            String desc = "(" + type.getDescriptor() + PUT_DESC_2;
            super.visitLdcInsn(f.getCodeId());
            super.visitLdcInsn(lastLine);
            superVisitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }

        private void tracePutField(FieldDescriptor f, ArgumentType type) {
            if (DEBUG) System.out.println("F " + f);
//            if (isInit) {
//                return;
//            }
            if (type.getWidth() == 1) {
                // obj, val
                super.visitInsn(DUP2); // obj, val, obj, val
            } else {
                // obj, val1, val2
                super.visitInsn(DUP2_X1); // val1, val2, obj, val1, val2
                super.visitInsn(POP2);  // val1, val2, obj
                super.visitInsn(DUP_X2); // obj, val1, val2, obj
                super.visitInsn(DUP_X2); // obj, obj, val1, val2, obj
                super.visitInsn(POP); // obj, obj, val1, val2
                super.visitInsn(DUP2_X1); // obj, val1, val2, obj, val1, val2
            }
            String method = PUT_ + type.getKey();
            String desc = PUT_DESC_1 + type.getDescriptor() + PUT_DESC_2;
            super.visitLdcInsn(f.getCodeId());
            super.visitLdcInsn(lastLine);
            superVisitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }
        
        private void traceGet(FieldDescriptor f, ArgumentType type, boolean inst) {
            if (DEBUG) System.out.println("G " + f + (inst?"":" static"));
            super.visitInsn(inst ? type.DUPw_X1() : type.DUPw());
            String method = GET_ + type.getKey();
            String desc = inst ? GET_DESC_1 : "(" ;
            desc += type.getDescriptor() + GET_DESC_2;
            super.visitLdcInsn(f.getCodeId());
            super.visitLdcInsn(lastLine);
            superVisitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }
        
        private void traceAStore(ArgumentType type) {
            if (DEBUG) System.out.println("[<- " + type);
            // array, index, value
            super.visitInsn(type.DUPw_X2()); // value, array, index, value
            super.visitInsn(type.POPw());    // value, array, index
            super.visitInsn(type.DUP2_Xw()); // array, index, value, array, index
            super.visitInsn(type.DUP2_Xw()); // array, index, array, index, value, array, index
            super.visitInsn(POP2);           // array, index, array, index, value
            super.visitInsn(type.DUPw_X2()); // array, index, value, array, index, value
            
            String method = ASTORE_ + type.getKey();
            String desc = ASTORE_DESC_1 + type.getDescriptor() + ASTORE_DESC_2;
            super.visitLdcInsn(lastLine);
            superVisitMethodInsn(INVOKESTATIC, TRACER, method, desc, false);
        }
        
        private void traceALoad(ArgumentType type) {
            if (DEBUG) System.out.println("[-> " + type);
            // (array, index,) value
            super.visitInsn(type.DUPw_X2()); // value, array, index, value
            
            String method = ALOAD_ + type.getKey();
            String desc = ALOAD_DESC_1 + type.getDescriptor() + ALOAD_DESC_2;
            super.visitLdcInsn(lastLine);
            superVisitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }

        @Override
        public void visitInsn(int opcode) {
            if (!tracingEnabled) {
                super.visitInsn(opcode);
                return;
            }
            switch (opcode) {
                case RETURN: traceReturn(ArgumentType.VOID); break;
                case IRETURN: traceReturn(ArgumentType.INT); break;
                case LRETURN: traceReturn(ArgumentType.LONG); break;
                case FRETURN: traceReturn(ArgumentType.FLOAT); break;
                case DRETURN: traceReturn(ArgumentType.DOUBLE); break;
                case ARETURN: traceReturn(ArgumentType.OBJECT); break;
                case ATHROW: traceThrow(); break;
                case BASTORE: traceAStore(ArgumentType.BYTE); break;
                case CASTORE: traceAStore(ArgumentType.CHAR); break;
                case SASTORE: traceAStore(ArgumentType.SHORT); break;
                case IASTORE: traceAStore(ArgumentType.INT); break;
                case LASTORE: traceAStore(ArgumentType.LONG); break;
                case FASTORE: traceAStore(ArgumentType.FLOAT); break;
                case DASTORE: traceAStore(ArgumentType.DOUBLE); break;
                case AASTORE: traceAStore(ArgumentType.OBJECT); break;
                case BALOAD:
                case CALOAD:
                case SALOAD:
                case IALOAD:
                case LALOAD:
                case FALOAD:
                case DALOAD:
                case AALOAD:
                    super.visitInsn(DUP2); // dup array and index
                    break;
            }
            super.visitInsn(opcode);
            switch (opcode) {
                case BALOAD: traceALoad(ArgumentType.BYTE); break;
                case CALOAD: traceALoad(ArgumentType.CHAR); break;
                case SALOAD: traceALoad(ArgumentType.SHORT); break;
                case IALOAD: traceALoad(ArgumentType.INT); break;
                case LALOAD: traceALoad(ArgumentType.LONG); break;
                case FALOAD: traceALoad(ArgumentType.FLOAT); break;
                case DALOAD: traceALoad(ArgumentType.DOUBLE); break;
                case AALOAD: traceALoad(ArgumentType.OBJECT); break;
            }
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (!tracingEnabled) {
                super.visitVarInsn(opcode, var);
                return;
            }
            switch (opcode) {
                case ISTORE: traceStore(ArgumentType.INT, var); break;
                case LSTORE: traceStore(ArgumentType.LONG, var); break;
                case FSTORE: traceStore(ArgumentType.FLOAT, var); break;
                case DSTORE: traceStore(ArgumentType.DOUBLE, var); break;
                case ASTORE: traceStore(ArgumentType.OBJECT, var); break;
            }
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (!tracingEnabled) {
                super.visitFieldInsn(opcode, owner, name, desc);
                return;
            }
            TypeDescriptor tOwner = model.getType(owner.replace('/', '.'), cl);
            FieldDescriptor f = tOwner.getField(name, desc);
            ArgumentType t = ArgumentType.getByDescriptor(desc);
            switch (opcode) {
                case PUTFIELD: tracePutField(f, t); break;
                case PUTSTATIC: tracePutStatic(f, t); break;
                case GETFIELD: super.visitInsn(DUP); break;
            }
            super.visitFieldInsn(opcode, owner, name, desc);
            switch (opcode) {
                case GETFIELD: traceGet(f, t, true); break;
                case GETSTATIC: traceGet(f, t, false); break;
            }
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            exHandlers.add(handler);
            super.visitTryCatchBlock(start, end, handler, type);
        }
        
        private void traceArgs() {
            ensureLineNumberIsTraced();
            int var = 0;
            if (!isStatic) {
                argTypes.add(null);
                var++; // dont trace `this`
            }
            int d = 1;
            while (descriptor.charAt(d) != ')') {
                ArgumentType t = ArgumentType.getByDescriptor(descriptor.charAt(d));
                if (t == null) 
                    throw new IllegalArgumentException(descriptor.substring(d));
                int dEnd;
                if (t == ArgumentType.OBJECT) dEnd = endOfObjectDescriptor(descriptor, d);
                else dEnd = d+1;
                argTypes.add(descriptor.substring(d, dEnd));
                traceArg(t, var);
                d = dEnd;
                var += t.getWidth();
            }
        }
        
        private int endOfObjectDescriptor(String descriptor, int d) {
            while (descriptor.charAt(d) == '[') d++;
            if (descriptor.charAt(d) == 'L') d = descriptor.indexOf(';', d);
            return d+1;
        }

        private void traceThrow() {
            super.visitInsn(DUP);
            super.visitLdcInsn(lastLine);
            superVisitMethodInsn(INVOKESTATIC, TRACER, THROW, THROW_DESC);
        }

        private void traceCatch() {
            super.visitInsn(DUP);
            super.visitLdcInsn(lastLine);
            superVisitMethodInsn(INVOKESTATIC, TRACER, CATCH, CATCH_DESC);            
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack+6, maxLocals);
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            super.visitFrame(type, nLocal, local, nStack, stack);
        }
    }
    
    static enum ArgumentType {
        
        INT('I'),
        LONG('L'),
        FLOAT('F'),
        DOUBLE('D'),
        OBJECT('A'),
        VOID('V'),
        BYTE('B'),
        CHAR('C'),
        SHORT('S'),;
        
        private final char key;
        private final String descriptor;
        private final int width;

        private ArgumentType(char key) {
            this.key = key;
            
            if (key == 'L') descriptor = "J";
            else if (key == 'A') descriptor = "Ljava/lang/Object;";
            else descriptor = String.valueOf(key);
            
            if (key == 'L' || key == 'D') width = 2;
            else width = 1;
        }

        public char getKey() {
            return key;
        }
        
        public String getDescriptor() {
            return descriptor;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int POPw() {
            return width == 1 ? Opcodes.POP : Opcodes.POP2;
        }
        
        public int DUPw() {
            return width == 1 ? Opcodes.DUP : Opcodes.DUP2;
        }
        
        public int DUPw_Xw() {
            return width == 1 ? Opcodes.DUP_X1 : Opcodes.DUP2_X2;
        }
        
        public int DUPw_X1() {
            return width == 1 ? Opcodes.DUP_X1 : Opcodes.DUP2_X1;
        }
        
        public int DUPw_X2() {
            return width == 1 ? Opcodes.DUP_X2 : Opcodes.DUP2_X2;
        }
        
        public int DUP2_Xw() {
            return width == 1 ? Opcodes.DUP2_X1 : Opcodes.DUP2_X2;
        }
        
        private static final int O_BYTE = 6;//BYTE.ordinal();
        
        private boolean isSubInt() {
            return ordinal() >= O_BYTE;
        }
        
        public int LOAD() {
            int o = ordinal();
            if (isSubInt()) return INT.LOAD();
            return Opcodes.ILOAD + o;
        }
        
        private static final ArgumentType[] VALUES = values();
        
        public static ArgumentType get(char key) {
            for (ArgumentType t: VALUES) {
                if (t.getKey() == key) return t;
            }
            return null;
        }
        
        public static ArgumentType getByDescriptor(String desc) {
            ArgumentType t = getByDescriptor(desc.charAt(0), false);
            if (t == null) throw new IllegalArgumentException(desc);
            return t;
        }
        
        public static ArgumentType getByDescriptor(char key) {
            return getByDescriptor(key, false);
        }
        
        public static ArgumentType getByDescriptor(char key, boolean explicitByteBoolean) {
            switch (key) {
                case 'B':
                case 'Z': if (explicitByteBoolean) return BYTE;
                case 'C':
                case 'I':
                case 'S': return INT;
                case 'J': return LONG;
                case 'F': return FLOAT;
                case 'D': return DOUBLE;
                case '[':
                case 'L': return OBJECT;
            }
            return null;
        }
        
    }
    
    public static class MyClassWriter extends ClassWriter {
        
        final ClassLoader cl;

        public MyClassWriter(ClassLoader cl, int flags) {
            super(flags);
            this.cl = cl;
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                return superGetCommonSuperClass(type1, type2);
            } catch (RuntimeException re) {
                throw new RuntimeException(type1 + " " + type2, re);
            }
        }
        
        protected String superGetCommonSuperClass(final String type1, final String type2) {
            try {
                Class<?> c, d;
                ClassLoader classLoader = cl;
                String type1d = type1.replace('/', '.');
                String type2d = type2.replace('/', '.');
                c = getKnownClass(classLoader, type1d);
                d = getKnownClass(classLoader, type2d);
                if (d == null) {
                    return type1;
                }
                if (c == null) {
                    return type2;
                }
                if (c.isAssignableFrom(d)) {
                    return type1;
                }
                if (d.isAssignableFrom(c)) {
                    return type2;
                }
                if (c.isInterface() || d.isInterface()) {
                    return "java/lang/Object";
                } else {
                    do {
                        c = c.getSuperclass();
                    } while (!c.isAssignableFrom(d));
                    return c.getName().replace('.', '/');
                }
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e.toString());
            }
        }
        
    }
    
    public static interface TraceFilter {

        public Object visitClass(String name, String superName);

        public Object visitMethod(String name, Object classFilterData);

        public boolean isTraceEntry(String name, Object traceFilterData);

        public boolean isTraceEntryAt(String desc, boolean visible, boolean isEntry, Object traceFilterData);
    }
    
    public static class MainMethodTraceFilter implements TraceFilter {

        @Override
        public Object visitClass(String name, String superName) {
            return null;
        }

        @Override
        public Object visitMethod(String name, Object classFilterData) {
            return (Boolean) name.equals("main");
        }

        @Override
        public boolean isTraceEntry(String name, Object traceFilterData) {
            return (Boolean) traceFilterData;
        }

        @Override
        public boolean isTraceEntryAt(String desc, boolean visible, boolean isEntry, Object traceFilterData) {
            return isEntry;
        }   
    }
    
    public static class MainRunMethodTraceFilter implements TraceFilter {

        @Override
        public Object visitClass(String name, String superName) {
            return null;
        }

        @Override
        public Object visitMethod(String name, Object classFilterData) {
            return (Boolean) name.equals("main") || name.equals("run");
        }

        @Override
        public boolean isTraceEntry(String name, Object traceFilterData) {
            return (Boolean) traceFilterData;
        }

        @Override
        public boolean isTraceEntryAt(String desc, boolean visible, boolean isEntry, Object traceFilterData) {
            return isEntry;
        }
        
    }
    
    private static final Object TEST_CLASS_KEY = "test-class";
    private static final Object TEST_METHOD_KEY = "test-method";
    
    public static class TestTraceFilter implements TraceFilter {

        @Override
        public Object visitClass(String name, String superName) {
            if (name.endsWith("Test") || (superName != null && superName.contains("TestCase"))) {
                return TEST_CLASS_KEY;
            }
            return null;
        }

        @Override
        public Object visitMethod(String name, Object classFilterData) {
            if (classFilterData == TEST_CLASS_KEY) {
                if (name.startsWith("test")) {
                    return TEST_METHOD_KEY;
                }
                return TEST_CLASS_KEY;
            }
            return null;
        }

        @Override
        public boolean isTraceEntry(String name, Object methodFilterData) {
            return methodFilterData == TEST_METHOD_KEY;
        }

        @Override
        public boolean isTraceEntryAt(String desc, boolean visible, boolean isEntry, Object methodFilterData) {
            return isEntry || (methodFilterData == TEST_CLASS_KEY && desc.endsWith("/Test;"));
        }
        
    }
    
}
