package de.hpi.accidit.asmtracer;

import de.hpi.accidit.model.*;
import de.hpi.accidit.trace.Tracer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;
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
        boolean trace = Tracer.pauseTracing();
        try {
            for (String e: excludes) {
                if (className.startsWith(e))
                    return classfileBuffer;
            }
            System.out.println(">> " + className + "     " + loader);
            return transform(classfileBuffer);
        } finally {
            Tracer.resumeTracing(trace);
        }
    }

    public static byte[] transform(byte[] classfileBuffer) throws RuntimeException {
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
        
        public MyClassVisitor(ClassVisitor cv) {
            super(ASM4, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            isTestClass = name.endsWith("Test");
            type = Tracer.model.getType(name.replace('/', '.'));
            type.addSuper(superName);
            for (String iface: interfaces)
                type.addSuper(iface);
            type.supersCompleted();
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
        private static final String THROW = "thrown";
        private static final String THROW_DESC = "(Ljava/lang/Object;I)V";
        private static final String CATCH = "caught";
        private static final String CATCH_DESC = "(Ljava/lang/Object;I)V";

        
        private static final String BEGIN = "begin";
        private static final String BEGIN_DESC = "(I)V";
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

        public MyMethodVisitor(int access, String name, String desc, MethodVisitor mv, boolean testclass, TypeDescriptor type) {
            super(ASM4, mv);
            this.name = name;
            this.descriptor = desc;
            test = testclass && name.startsWith("test");
            //System.out.println("- " + name + " " + (test ? "t" : ""));
            this.type = type;
            this.me = type.getMethod(name, desc);
            if (DEBUG) System.out.println("\n" + name);
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
        }

        @Override
        public void visitLabel(Label label) {
            super.visitLabel(label);
            if (DEBUG) System.out.println("L " + label.getOffset());
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            lastLine = line;
            super.visitLineNumber(line, start);
            if (exHandlers.remove(start)) {
                traceCatch();
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (lastTracedLine != lastLine) {
                super.visitLdcInsn(lastLine);
                lastTracedLine = lastLine;
                super.visitMethodInsn(INVOKESTATIC, TRACER, LINE, LINE_DESC);
            }
            super.visitMethodInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            me.variablesCompleted();
        }

        private void traceReturn(ArgumentType type) {
            if (DEBUG) System.out.println("R " + type);
            String desc;
            String method = RETURN_ + type.getKey();
            if (type == ArgumentType.VOID) {
                desc = "(" + RETURN_DESC_2;
            } else {
                super.visitInsn(type.DUP());
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
            super.visitInsn(type.DUP());
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
            super.visitInsn(type.DUP());
            String method = PUT_ + type.getKey();
            String desc = "(" + type.getDescriptor() + PUT_DESC_2;
            super.visitLdcInsn(f.getCodeId());
            super.visitLdcInsn(lastLine);
            super.visitMethodInsn(INVOKESTATIC, TRACER, method, desc);
        }

        private void tracePutField(FieldDescriptor f, ArgumentType type) {
            if (DEBUG) System.out.println("F " + f);
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
            super.visitInsn(inst ? type.DUP_X1() : type.DUP());
            String method = GET_ + type.getKey();
            String desc = inst ? GET_DESC_1 : "(" ;
            desc += type.getDescriptor() + GET_DESC_2;
            super.visitLdcInsn(f.getCodeId());
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
                case ATHROW: traceThrow();
                    
            }
            super.visitInsn(opcode);
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
            TypeDescriptor tOwner = Tracer.model.getType(owner);
            FieldDescriptor f = tOwner.getField(name, desc);
            ArgumentType t = ArgumentType.getByDescriptor(desc.charAt(0));
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
            int var = 0;
            if (isInit) {
                var++; // trace `this` later
            } else if (!isStatic) {
                traceArg(ArgumentType.OBJECT, var); 
                var++;
            }
            int d = 1;
            while (descriptor.charAt(d) != ')') {
                ArgumentType t = ArgumentType.getByDescriptor(descriptor.charAt(d));
                if (t == ArgumentType.OBJECT) d = descriptor.indexOf(';', d);
                traceArg(t, var);
                d++;
                var++;
            }
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
        VOID('V');
        
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
        
        public int DUP() {
            return width == 1 ? Opcodes.DUP : Opcodes.DUP2;
        }
        
        public int DUP_X1() {
            return width == 1 ? Opcodes.DUP_X1 : Opcodes.DUP2_X2;
        }
        
        public int LOAD() {
            return Opcodes.ILOAD + ordinal();
        }
        
        private static final ArgumentType[] VALUES = values();
        
        public static ArgumentType get(char key) {
            for (ArgumentType t: VALUES) {
                if (t.getKey() == key) return t;
            }
            return null;
        }
        
        public static ArgumentType getByDescriptor(char key) {
            for (ArgumentType t: VALUES) {
                if (t.getDescriptor().charAt(0) == key) return t;
            }
            return null;
        }
        
    }
}
