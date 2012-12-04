package de.hpi.accidit.asmtracer;

import de.hpi.accidit.model.*;
import de.hpi.accidit.trace.Tracer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.Callable;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;


public class TracerTransformer implements ClassFileTransformer {
    
    private static String[] excludes = new ArrayList<String>(){{
        add("de/hpi/accidit/asm");
        add("de/hpi/accidit/model");
        add("de/hpi/accidit/trace");
        add("de/hpi/accidit/out");
        add("org/objectweb/asm");
        add("$");
        add("sun");
        add("java/lang");
        add("java/util/regex");
//        add("java/lang/Enum");
//        add("java/lang/Shut");
//        add("java/lang/Shut");
    }}.toArray(new String[0]);

    @Override
    public byte[] transform(ClassLoader loader, String className, 
                        Class<?> classBeingRedefined, 
                        ProtectionDomain protectionDomain, 
                        byte[] classfileBuffer) 
                            throws IllegalClassFormatException {
        try {
            return Tracer.noTrace(new TransformCall(loader, className, classfileBuffer));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] transformUntraced(String className, byte[] classfileBuffer, ClassLoader loader) throws RuntimeException {
        for (String e: excludes) {
            if (className.startsWith(e))
                return classfileBuffer;
        }
        System.out.println(">> " + className + "     " + loader);
        return transform(classfileBuffer, loader);
    }
    
    private class TransformCall implements Callable<byte[]> {
        private ClassLoader loader;
        private String className;
        private byte[] buffer;

        public TransformCall(ClassLoader loader, String className, byte[] buffer) {
            this.loader = loader;
            this.className = className;
            this.buffer = buffer;
        }
        
        @Override
        public byte[] call() throws Exception {
            return transformUntraced(className, buffer, loader);
        }
    }

    public static byte[] transform(byte[] classfileBuffer, ClassLoader cl) throws RuntimeException {
        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
            CheckClassAdapter cca = new CheckClassAdapter(cw, false);
            ClassVisitor transform = new MyClassVisitor(cca);
            cr.accept(transform, 0);
//            TracerTransformer2.tranform(cr, cw);
            
            return cw.toByteArray();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    static class MyClassVisitor extends ClassVisitor implements Opcodes {

        boolean isTestClass;
        TypeDescriptor type;
        final ClassLoader cl=null;
        
        public MyClassVisitor(ClassVisitor cv) {
            super(ASM4, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            isTestClass = name.endsWith("Test");
            type = Tracer.model.getType(name.replace('/', '.'));
            type.addSuper(superName.replace('/', '.'));
            for (String iface: interfaces)
                type.addSuper(iface.replace('/', '.'));
            type.initCompleted();
        }
        
        @Override
        public void visitAttribute(Attribute attr) {
            super.visitAttribute(attr);
        }

        private boolean init;
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor sup = super.visitMethod(access, name, desc, signature, exceptions);
            if ((access & (ACC_NATIVE | ACC_BRIDGE | ACC_ABSTRACT | ACC_INTERFACE)) != 0) {
                return sup;
            }
            if (name.equals("finalize") || name.equals("<clinit>")) {
                return sup;
            }
            return new MyMethodVisitor(access, name, desc, sup, isTestClass, type);
        }
        
    }
    
    static class MyMethodVisitor extends MethodVisitor implements Opcodes {
        
        private static final boolean DEBUG = true;
        
        private static final String TRACER = "de/hpi/accidit/trace/Tracer";
        private static final String LINE = "line";
        private static final String LINE_DESC = "(I)V";
        private static final String ENTER = "enter";
        private static final String ENTER_DESC = "(I)V";
        private static final String RETURN_ = "return";
        private static final String RETURN_DESC_2 = "I)V";
        private static final String STORE_ = "store";
        private static final String STORE_DESC_2 = "II)V";
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
        private static final String ALOAD_ = "aStore";
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
        
        private final String name;
        private final String descriptor;
        private final boolean isStatic;
        private final boolean isInit;
        private boolean test;
        private final TypeDescriptor type;
        private final MethodDescriptor me;
        private int lastLine = -1;
        private int lastTracedLine = -2;
        
        private final Set<Label> exHandlers = new HashSet<>();
        private final List<String> argTypes = new ArrayList<>();

        public MyMethodVisitor(int access, String name, String desc, MethodVisitor mv, boolean testclass, TypeDescriptor type) {
            super(ASM4, mv);
            this.name = name;
            this.descriptor = desc;
            test = testclass && name.startsWith("test");
            //System.out.println("- " + name + " " + (test ? "t" : ""));
            this.type = type;
            this.me = type.getMethod(name, desc);
            if (DEBUG) System.out.println("\n" + name + desc);
            isStatic = (access & ACC_STATIC) != 0;
            isInit = name.equals("<init>");
        }
        
        @Override
        public void visitCode() {
            super.visitCode();
            if (test) {
                super.visitMethodInsn(INVOKESTATIC, TRACER, BEGIN, BEGIN_DESC);
            }
            super.visitLdcInsn(me.getCodeId());
            super.visitMethodInsn(INVOKESTATIC, TRACER, ENTER, ENTER_DESC);
            traceArgs();
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            if (DEBUG) System.out.println(index + " " + desc + "/" + signature + " " + name + " " + start.getOffset() + "-" + end.getOffset());
            super.visitLocalVariable(name, desc, signature, start, end, index);
            String type = org.objectweb.asm.Type.getType(desc).getClassName();
            me.addVariable(index, name, type);
            if (index < argTypes.size()) argTypes.set(index, null);
        }

        @Override
        public void visitLabel(Label label) {
            super.visitLabel(label);
            if (DEBUG) System.out.println("L " + label.getOffset());
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (DEBUG) System.out.println("# " + line);
            lastLine = line;
            super.visitLineNumber(line, start);
            if (exHandlers.remove(start)) {
                traceCatch();
            }
        }
        
        private void ensureLineNumberIsTraced() {
            if (lastTracedLine != lastLine) {
                super.visitLdcInsn(lastLine);
                lastTracedLine = lastLine;
                super.visitMethodInsn(INVOKESTATIC, TRACER, LINE, LINE_DESC);
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            ensureLineNumberIsTraced();
            super.visitMethodInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            
            for (int i = 0; i < argTypes.size(); i++) {
                String argType = argTypes.get(i);
                if (argType != null) {
                    if (DEBUG) System.out.println(i + " " + argType + " ??");
                    String argName = TypeDescriptor.descriptorToName(argType);
                    me.addVariable(i, "$"+i, argName);
                }
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
            super.visitMethodInsn(INVOKESTATIC, TRACER, method, desc);

            if (test) {
                super.visitMethodInsn(INVOKESTATIC, TRACER, END, END_DESC);
            }
        }

        private void traceStore(ArgumentType type, int var) {
            if (DEBUG) System.out.println("S " + type + " " + var);
            String desc;
            String method = STORE_ + type.getKey();
            super.visitInsn(type.DUPw());
            desc = "(" + type.getDescriptor() + STORE_DESC_2;
            super.visitLdcInsn(var);
            super.visitLdcInsn(lastLine);
            super.visitMethodInsn(INVOKESTATIC, TRACER, method, desc);          
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
            super.visitMethodInsn(INVOKESTATIC, TRACER, method, desc);          
        }

        private void tracePutStatic(FieldDescriptor f, ArgumentType type) {
            if (DEBUG) System.out.println("F " + f + " static");
            super.visitInsn(type.DUPw());
            String method = PUT_ + type.getKey();
            String desc = "(" + type.getDescriptor() + PUT_DESC_2;
            super.visitLdcInsn(f.getCodeId());
            super.visitLdcInsn(lastLine);
            super.visitMethodInsn(INVOKESTATIC, TRACER, method, desc);
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
            super.visitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }
        
        private void traceGet(FieldDescriptor f, ArgumentType type, boolean inst) {
            if (DEBUG) System.out.println("G " + f + (inst?"":" static"));
            super.visitInsn(inst ? type.DUPw_X1() : type.DUPw());
            String method = GET_ + type.getKey();
            String desc = inst ? GET_DESC_1 : "(" ;
            desc += type.getDescriptor() + GET_DESC_2;
            super.visitLdcInsn(f.getCodeId());
            super.visitLdcInsn(lastLine);
            super.visitMethodInsn(INVOKESTATIC, TRACER, method, desc);
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
            super.visitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }
        
        private void traceALoad(ArgumentType type) {
            if (DEBUG) System.out.println("[-> " + type);
            // (array, index,) value
            super.visitInsn(type.DUPw_X2()); // value, array, index, value
            
            String method = ALOAD_ + type.getKey();
            String desc = ALOAD_DESC_1 + type.getDescriptor() + ALOAD_DESC_2;
            super.visitLdcInsn(lastLine);
            super.visitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }

        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
                case RETURN: traceReturn(ArgumentType.VOID); break;
                case IRETURN: traceReturn(ArgumentType.INT); break;
                case LRETURN: traceReturn(ArgumentType.LONG); break;
                case FRETURN: traceReturn(ArgumentType.FLOAT); break;
                case DRETURN: traceReturn(ArgumentType.DOUBLE); break;
                case ARETURN: traceReturn(ArgumentType.OBJECT); break;
                case ATHROW: traceThrow(); break;
                case BASTORE: traceAStore(ArgumentType.BYTE); break;
                case IASTORE: traceAStore(ArgumentType.INT); break;
                case LASTORE: traceAStore(ArgumentType.LONG); break;
                case FASTORE: traceAStore(ArgumentType.FLOAT); break;
                case DASTORE: traceAStore(ArgumentType.DOUBLE); break;
                case AASTORE: traceAStore(ArgumentType.OBJECT); break;
                case BALOAD:
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
                case IALOAD: traceALoad(ArgumentType.INT); break;
                case LALOAD: traceALoad(ArgumentType.LONG); break;
                case FALOAD: traceALoad(ArgumentType.FLOAT); break;
                case DALOAD: traceALoad(ArgumentType.DOUBLE); break;
                case AALOAD: traceALoad(ArgumentType.OBJECT); break;
            }
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
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
            TypeDescriptor tOwner = Tracer.model.getType(owner.replace('/', '.'));
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
            if (isInit) {
//                argTypes.add("--this--");
//                var++; // trace `this` later
            } else if (!isStatic) {
                argTypes.add("--this--");
                traceArg(ArgumentType.OBJECT, var); 
                var++;
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
                var++;
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
            super.visitMethodInsn(INVOKESTATIC, TRACER, THROW, THROW_DESC);
        }

        private void traceCatch() {
            super.visitInsn(DUP);
            super.visitLdcInsn(lastLine);
            super.visitMethodInsn(INVOKESTATIC, TRACER, CATCH, CATCH_DESC);            
        }
        
    }
    
    
    static enum ArgumentType {
        
        INT('I'),
        LONG('L'),
        FLOAT('F'),
        DOUBLE('D'),
        OBJECT('A'),
        VOID('V'),
        BYTE('B');
        
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
        
        public int LOAD() {
            if (this == BYTE) return INT.LOAD();
            return Opcodes.ILOAD + ordinal();
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
}
