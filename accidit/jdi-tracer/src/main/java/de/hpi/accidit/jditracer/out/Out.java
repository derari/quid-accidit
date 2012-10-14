package de.hpi.accidit.jditracer.out;

import de.hpi.accidit.jditracer.model.TypeDescriptor;
import de.hpi.accidit.jditracer.model.TestTrace;
import de.hpi.accidit.jditracer.model.LocalVarDescriptor;
import de.hpi.accidit.jditracer.model.FieldDescriptor;
import de.hpi.accidit.jditracer.model.MethodDescriptor;
import de.hpi.accidit.jditracer.model.ObjectTrace;

/**
 *
 * @author Arian Treffer
 */
public interface Out extends AutoCloseable {

    public void type(TypeDescriptor t);
    
    public void supers(TypeDescriptor t);

    public void method(MethodDescriptor m);

    public void field(FieldDescriptor f);

    public void local(LocalVarDescriptor lv);

//    public void source(SourceDescriptor s);
    
    public void test(TestTrace t);
    
    public void object(ObjectTrace o);

    public void testData(TestTrace t);

}
