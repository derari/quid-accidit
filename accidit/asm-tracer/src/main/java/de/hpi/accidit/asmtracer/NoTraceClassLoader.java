package de.hpi.accidit.asmtracer;

import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class NoTraceClassLoader extends ClassLoader {
    
    private final Set<String> classes = new HashSet<>();

    public NoTraceClassLoader(ClassLoader parent) {
        super(parent);
    }
    
    public void addClass(String cls) {
        classes.add(cls.replace('.', '/'));
    }
    
    public void addClasses(Class... cls) {
        for (Class c: cls)
            addClass(c.getName());
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (classes.remove(name.replace('.', '/'))) {
            return loadClassUntraced(name, resolve);
        }
        return super.loadClass(name, resolve);
    }

    private Class<?> loadClassUntraced(String name, boolean resolve) throws ClassNotFoundException {
        URL url = getResource(name.replace('.', '/') + ".class");
        byte[] data = loadUrl(url);
        return defineClass(name, data, 0, data.length);
    }
    
    private static byte[] loadUrl(URL url) throws ClassNotFoundException {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();
        try (InputStream is = url.openStream()) {
            byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
            int n;
            while ((n = is.read(byteChunk)) > 0) {
                bais.write(byteChunk, 0, n);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(String.valueOf(url), e);
        } 
        return bais.toByteArray();
    }

    
    
    
}
