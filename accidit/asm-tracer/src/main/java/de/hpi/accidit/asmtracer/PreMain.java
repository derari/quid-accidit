package de.hpi.accidit.asmtracer;

import de.hpi.accidit.model.Model;
import de.hpi.accidit.out.CsvOut;
import de.hpi.accidit.out.Out;
import de.hpi.accidit.trace.*;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

public class PreMain {

    private static String traceRoot = "target/trace";
    private static boolean timestampDir = true;
    private static String resultFile = null;
    
    public static void premain(String argString, Instrumentation inst) throws Exception {
        try {
            String[] args = argString.split("#");
            for (String jar: args[0].split(";")) {
                JarFile jarFile = new JarFile(jar);
                inst.appendToBootstrapClassLoaderSearch(jarFile);
            }
            if (args.length > 1) {
//                System.out.println("Mode: " + args[1]);
                switch (args[1].toLowerCase()) {
                    case "":
                    case "main":
                        TracerTransformer.TRACE_FILTER = new TracerTransformer.MainMethodTraceFilter();
                        break;
                    case "main+run":
                        TracerTransformer.TRACE_FILTER = new TracerTransformer.MainRunMethodTraceFilter();
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
//                System.out.println("Output: " + args[2]);
                traceRoot = args[2];
            }
            if (args.length > 3) {
                String a = args[3].toLowerCase();
                timestampDir = a.equals("1") || a.startsWith("t") || a.startsWith("y");
//                System.out.println("Timestamp: " + args[3] + "(" + timestampDir + ")");
            }
            if (args.length > 4) {
                resultFile = args[4];
                Runtime.getRuntime().addShutdownHook(new Thread(){
                    @Override
                    public void run() {
                        File f = new File(resultFile);
                        f.getParentFile().mkdirs();
                        try (PrintWriter pw = new PrintWriter(f)) {
                            pw.append("ok");
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                    }
                });
            }

            PreMain.inst = inst;
            Init.init(inst);
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    public static Instrumentation inst;
    
    public static class Init {
        
        public static final Set<Class> TO_BE_REDEFINED = new LinkedHashSet<>();

        private static void init(final Instrumentation inst) throws Exception {
            TracerSetup.setTraceSet(new TraceSet(new Model(createOut())));
            Tracer.setup(() -> {
                try {
                    TO_BE_REDEFINED.add(String.class);
                    TracerTransformer.fullTracingEnabled = true;
                    for (Class c: inst.getAllLoadedClasses()) {
                        if (inst.isModifiableClass(c) && 
                                !c.getName().contains("Lambda")) {
                            TO_BE_REDEFINED.add(c);
                        }
                    }
                    final int step = 64;
                    final Class[] buf = new Class[step];
                    while (TO_BE_REDEFINED.size() > 0) {
                        System.out.println(TO_BE_REDEFINED.size() + " remaining ~~~~~~");
                        int len = Math.min(step, TO_BE_REDEFINED.size());
                        Arrays.fill(buf, len, buf.length, null);
                        Iterator<Class> it = TO_BE_REDEFINED.iterator();
                        for (int i = 0; i < len; i++) {
                            buf[i] = it.next();
                        }
                        try {
                            inst.retransformClasses(buf);
                        } catch (Throwable t) {
                            t.printStackTrace(System.err);
                            Arrays.asList(buf).forEach(c -> {
                                System.err.println(c);
                                TO_BE_REDEFINED.remove(c);
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                } finally {
                    System.out.println("---------------- DONE");
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
