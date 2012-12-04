package de.hpi.accidit.asmtracer;

import de.hpi.accidit.model.Model;
import de.hpi.accidit.out.PrintStreamOut;
import de.hpi.accidit.trace.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.junit.*;
import static org.junit.Assume.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import static org.hamcrest.Matchers.*;

public class AgentTest {
    
    static {
        TracerSetup.setTraceSet(new TraceSet(new Model(new PrintStreamOut())));
    }
    
    private static final String ACLASS = ASimpleTest.class.getCanonicalName();
    
    private static Class<?> aClass = null;
    
    private static Class loadAClass() throws IOException, ClassNotFoundException {
        AClassLoader cl = new AClassLoader(AgentTest.class.getClassLoader());
        loadClass(cl, ACLASS);
        loadClass(cl, ACLASS + "$Access");
        return Class.forName(ACLASS, true, cl);
    }
    
    private static void loadClass(AClassLoader cl, String name) throws IOException, ClassNotFoundException {
        ClassReader cr = new ClassReader(name);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        CheckClassAdapter cca = new CheckClassAdapter(cw, false);
        cr.accept(new TracerTransformer.MyClassVisitor(cca), 0);
        cl.setData(name, cw.toByteArray());
    }
    
    static Class aClass() throws Exception {
        if (aClass == null) {
            try {
                aClass = loadAClass();
            } catch (Throwable e) {
                aClass = Exception.class;
                throw e;
            }
        } else if (aClass == Exception.class) {
            assumeThat("failed", is("Instrumentation succeeded"));
        }
        return aClass;
    }
    
    protected Object runATest(String method) throws Exception {
        return runATest(method, false);
    }
    
    protected Object runATest(String method, boolean exceptionExpected) throws Exception {
        Class c = aClass();
        Object a = c.newInstance();
        Tracer.begin();
        try {
            return c.getMethod(method).invoke(a);
        } catch (InvocationTargetException e) {
            if (!exceptionExpected) throw e;
            Tracer.caught(e.getCause(), 0);
            return null;
        } finally {
            if (!exceptionExpected) Tracer.end();
        }
    }
    
    @Before
    public void setUp() {
        System.out.println();
    }
    
    @After
    public void tearDown() {
        System.out.println();
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

    @Test
    public void test_array() throws Exception {
        Object result = runATest("arrayTest");
        System.out.println(result);
    }

    @Test
    public void test_test() throws Exception {
        Object result = runATest("testTest");
        System.out.println(result);
    }

    @Test
    public void test_nested() throws Exception {
        Object result = runATest("nestedTest");
        System.out.println(result);
    }

    private static class AClassLoader extends ClassLoader {
        
        private final Map<String, byte[]> classes = new HashMap<>();

        public AClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            byte[] data = classes.remove(name);
            if (data != null) {
                try {
                    File f = new File("./target/test/"+name+".class");
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(data);
                    fos.flush();
                } catch (IOException e) { e.printStackTrace(); }
                return defineClass(name, data, 0, data.length);
            }
            return super.loadClass(name);
        }

        private void setData(String name, byte[] data) {
            classes.put(name, data);
        }
        
    }
    
}
