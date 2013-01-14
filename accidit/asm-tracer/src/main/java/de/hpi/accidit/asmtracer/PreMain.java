package de.hpi.accidit.asmtracer;

import de.hpi.accidit.model.Model;
import de.hpi.accidit.out.CsvOut;
import de.hpi.accidit.out.Out;
import de.hpi.accidit.trace.*;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class PreMain {

    public static void premain(String args, final Instrumentation inst) throws Exception {
        try {            
            for (String jar: args.split(";")) {
                JarFile jarFile = new JarFile(jar);
                inst.appendToBootstrapClassLoaderSearch(jarFile);
            }

            Init.init(inst);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static class Init {

        private static void init(final Instrumentation inst) throws Exception {
            TracerSetup.setTraceSet(new TraceSet(new Model(createOut())));

            Tracer.setup(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<Class> classes = new ArrayList<>();
                        for (Class c: inst.getAllLoadedClasses()) {
                            if (inst.isModifiableClass(c)) classes.add(c);
                        }
                        inst.retransformClasses(classes.toArray(new Class[0]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            TracerTransformer trans = new TracerTransformer();
            inst.addTransformer(trans, true);

        }

        public static Out createOut() throws Exception {
            return new CsvOut(new File("target/trace"));
//            try {
//                NoTraceClassLoader cl = new NoTraceClassLoader(PreMain.class.getClassLoader());
//                cl.addClasses(CsvOut.class, CsvOut.Csv.class);
//                Class cCsvOut = Class.forName(CsvOut.class.getName(), true, cl);
//                return (Out) cCsvOut.getConstructor(File.class).newInstance(new File("trace"));
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
        }
    }
}
