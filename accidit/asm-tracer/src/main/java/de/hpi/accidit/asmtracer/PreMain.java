package de.hpi.accidit.asmtracer;

import de.hpi.accidit.trace.Tracer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class PreMain {

    public static void premain(String args, final Instrumentation inst) throws Exception {
        TracerTransformer trans = new TracerTransformer();
        inst.addTransformer(trans, true);
        
        for (String jar: args.split(";")) {
            JarFile jarFile = new JarFile(jar);
            inst.appendToBootstrapClassLoaderSearch(jarFile);
        }
        
        Tracer.setup(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Class> classes = new ArrayList<>();
                    for (Class c: inst.getAllLoadedClasses()) {
                        if (inst.isModifiableClass(c)) classes.add(c);
                        if (c.getName().equals("java.lang.ArrayList")) {
                            System.out.println(c);
                        }
                    }
                    inst.retransformClasses(classes.toArray(new Class[0]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

//        File f = new File("../asm-tracer/target/accidit-asm-tracer-1.0-SNAPSHOT.jar");
//        JarFile jf = new JarFile(f.getAbsoluteFile());
//        inst.appendToBootstrapClassLoaderSearch(jf);        
    }
}
