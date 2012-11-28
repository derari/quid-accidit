package de.hpi.accidit.asmtracer;

/**
 *
 * @author derari
 */
public class TracerClassLoader extends ClassLoader {

    public TracerClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        System.out.println("?" + name);
        return super.findClass(name);
    }
    
}
