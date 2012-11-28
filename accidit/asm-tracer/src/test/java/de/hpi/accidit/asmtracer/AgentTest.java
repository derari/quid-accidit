package de.hpi.accidit.asmtracer;

import de.hpi.accidit.asmtracer.TracerTransformer;
import de.hpi.accidit.trace.Tracer;
import java.io.IOException;
import static org.junit.Assume.*;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import static org.hamcrest.Matchers.*;

public class AgentTest {
    
    private static final String ACLASS = "de.hpi.accidit.asmtracer.AClass";
    
    private static Class<?> aClass = null;
    
    private static Class loadAClass() throws IOException, ClassNotFoundException {
        ClassReader cr = new ClassReader(AClass.class.getCanonicalName());
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        CheckClassAdapter cca = new CheckClassAdapter(cw, false);
        cr.accept(new TracerTransformer.MyClassVisitor(cca), 0);
        byte[] data = cw.toByteArray();
        AClassLoader cl = new AClassLoader(data, AgentTest.class.getClassLoader());
        return Class.forName(ACLASS, true, cl);
    }
    
    static Class aClass() throws Exception {
        if (aClass == null) {
            try {
                aClass = loadAClass();
            } catch (Exception e) {
                aClass = Exception.class;
                throw e;
            }
        } else if (aClass == Exception.class) {
            assumeThat("failed", is("Instrumentation succeeded"));
        }
        return aClass;
    }
    
    protected Object runATest(String method) throws Exception {
        Class c = aClass();
        Object a = c.newInstance();
        Tracer.begin(0);
        try {
            return c.getMethod(method).invoke(a);
        } finally {
            Tracer.end();
        }
    }
    
    @Test
    public void test_basic() throws Exception {
        Object result = runATest("basicTest");
        System.out.println(result);
    }

    @Test
    public void test_ex() throws Exception {
        Object result = runATest("exTest");
        System.out.println(result);
    }

    @Test
    public void test_new() throws Exception {
        Object result = runATest("newTest");
        System.out.println(result);
    }

    private static class AClassLoader extends ClassLoader {
        
        private final byte[] aclass;

        public AClassLoader(byte[] aclass, ClassLoader parent) {
            super(parent);
            this.aclass = aclass;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.equals(ACLASS)) {
                return defineClass(ACLASS, aclass, 0, aclass.length);
            }
            return super.loadClass(name);
        }
        
    }
    
}
