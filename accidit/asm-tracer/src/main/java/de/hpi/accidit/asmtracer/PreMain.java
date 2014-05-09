package de.hpi.accidit.asmtracer;

import de.hpi.accidit.model.Model;
import de.hpi.accidit.out.CsvOut;
import de.hpi.accidit.out.Out;
import de.hpi.accidit.trace.*;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.jar.JarFile;

public class PreMain {

    private static String traceRoot = "target/trace";
    private static boolean timestampDir = true;
    
    public static void premain(String argString, Instrumentation inst) throws Exception {
        try {
            String[] args = argString.split("#");
            for (String jar: args[0].split(";")) {
                JarFile jarFile = new JarFile(jar);
                inst.appendToBootstrapClassLoaderSearch(jarFile);
            }
            if (args.length > 1) {
                switch (args[1].toLowerCase()) {
                    case "":
                    case "main":
                        TracerTransformer.TRACE_FILTER = new TracerTransformer.MainMethodTraceFilter();
                        break;
                    case "test":
                        TracerTransformer.TRACE_FILTER = new TracerTransformer.TestTraceFilter();
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "unknown filter: " + args[1]);
                }
            }
            if (args.length > 2) {
                traceRoot = args[2];
            }
            if (args.length > 3) {
                String a = args[3].toLowerCase();
                timestampDir = a.equals("1") || a.startsWith("t") || a.startsWith("y");
            }

            PreMain.inst = inst;
            Init.init(inst);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static Instrumentation inst;
    
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
                        inst.retransformClasses(classes.toArray(new Class[classes.size()]));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            TracerTransformer trans = new TracerTransformer();
            inst.addTransformer(trans, true);

        }

        public static Out createOut() throws Exception {
            File f = new File(traceRoot);
            if (timestampDir) {
                GregorianCalendar c = new GregorianCalendar();
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-kkmmss");
                String s = df.format(c.getTime());
                f = new File(f, s);
            }
            return new CsvOut(f);
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
