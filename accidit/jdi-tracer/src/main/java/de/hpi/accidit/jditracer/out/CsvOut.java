package de.hpi.accidit.jditracer.out;

import de.hpi.accidit.jditracer.model.TypeDescriptor;
import de.hpi.accidit.jditracer.model.LocalVarDescriptor;
import de.hpi.accidit.jditracer.model.FieldAccessTrace;
import de.hpi.accidit.jditracer.model.ThrowTrace;
import de.hpi.accidit.jditracer.model.MethodDescriptor;
import de.hpi.accidit.jditracer.model.FieldDescriptor;
import de.hpi.accidit.jditracer.model.InvocationTrace;
import de.hpi.accidit.jditracer.model.ObjectTrace;
import de.hpi.accidit.jditracer.model.LocalVarTrace;
import de.hpi.accidit.jditracer.model.TestTrace;
import de.hpi.accidit.jditracer.model.FieldTrace;
import de.hpi.accidit.jditracer.model.CatchTrace;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 *
 * @author Arian Treffer
 */
public class CsvOut implements Out {
    
    private final PrintWriter mTypes;
    private final PrintWriter mExtends;
    private final PrintWriter mMethods;
    private final PrintWriter mFields;
    private final PrintWriter mLocals;
//    private final PrintWriter mSource;
    
    private final PrintWriter tTests;
    private final PrintWriter tInvocations;
    private final PrintWriter tLocals;
    private final PrintWriter tObjects;
    private final PrintWriter tFields;
    private final PrintWriter tAccesses;
    private final PrintWriter tThrows;
    private final PrintWriter tCatches;

    public CsvOut(File dir) throws FileNotFoundException {
        dir.mkdirs();
        mTypes = new PrintWriter(new File(dir, "mTypes.csv"));
        mExtends = new PrintWriter(new File(dir, "mExtends.csv"));
        mMethods = new PrintWriter(new File(dir, "mMethods.csv"));
        mFields = new PrintWriter(new File(dir, "mFields.csv"));
        mLocals = new PrintWriter(new File(dir, "mLocals.csv"));
//        mSource = new PrintWriter(new File(dir, "mSource.csv"));
        
        tTests = new PrintWriter(new File(dir, "tTests.csv"));
        tInvocations = new PrintWriter(new File(dir, "tInvocations.csv"));
        tLocals = new PrintWriter(new File(dir, "tLocals.csv"));
        tObjects = new PrintWriter(new File(dir, "tObjects.csv"));
        tFields = new PrintWriter(new File(dir, "tFields.csv"));
        tAccesses = new PrintWriter(new File(dir, "tAccesses.csv"));
        tThrows = new PrintWriter(new File(dir, "tThrows.csv"));
        tCatches = new PrintWriter(new File(dir, "tCatches.csv"));
    }

    @Override
    public void close() throws Exception {
        mTypes.close();
        mExtends.close();
        mMethods.close();
        mFields.close();
        mLocals.close();
//        mSource.close();
        
        tTests.close();
        tInvocations.close();
        tLocals.close();
        tObjects.close();
        tFields.close();
        tAccesses.close();
        tThrows.close();
        tCatches.close();
    }
    
    public synchronized void flush() {
        mTypes.flush();
        mExtends.flush();
        mMethods.flush();
        mFields.flush();
        mLocals.flush();
//        mSource.flush();
        
        tTests.flush();
        tInvocations.flush();
        tLocals.flush();
        tObjects.flush();
        tFields.flush();
        tAccesses.flush();
        tThrows.flush();
        tCatches.flush();
    }
    
    private void csv(final PrintWriter pw, Object... values) {
        synchronized (pw) {
            boolean first = true;
            for (Object v: values) {
                if (first) first = false;
                else pw.append(';');
                if (v == null) {
                    pw.print("NULL");
                } else {
                    String s = v.toString();
                    if (s.equals("NULL")) pw.print("'NULL'");
                    else pw.print(v);
    //                pw.print(v);
                }
            }
            pw.print('\n'); // unix line ends
        }
    }
    
    private String quote(Object o) {
        return '\'' + String.valueOf(o) + '\'';
    }

    @Override
    public void type(TypeDescriptor t) {
        csv(mTypes, 
                t.getId(), 
                t.getName(),
                t.getFile());
    }

    @Override
    public void supers(TypeDescriptor t) {
        final int tId = t.getId();
        for (TypeDescriptor sup: t.getSupers()) {
            csv(mExtends, 
                    tId, 
                    sup.getId());
        }
    }
    
    @Override
    public void method(MethodDescriptor m) {
        csv(mMethods, 
                m.getId(), 
                m.getDeclaringType().getId(), 
                m.getName(),
                quote(m.getSignature()));
    }

    @Override
    public void field(FieldDescriptor f) {
        csv(mFields,
                f.getId(),
                f.getDeclaringType().getId(),
                f.getName());
    }

    @Override
    public void local(LocalVarDescriptor lv) {
        csv(mLocals,
                lv.getMethod().getId(),
                lv.getId(),
                lv.getName(),
                lv.isArg() ? 1 : 0);
    }

//    @Override
//    public void source(SourceDescriptor s) {
//        csv(mSource,
//                s.getId(),
//                s.getFile());
//    }
    
    @Override
    public void test(TestTrace t) {
        csv(tTests,
                t.getId(),
                quote(t.getName()));
    }

    @Override
    public void object(ObjectTrace o) {
        csv(tObjects,
                o.getTestId(),
                o.getId(),
                o.getType().getId());
    }

    @Override
    public void testData(TestTrace t) {
        invocations(t);
        flush();
    }

    private void invocations(TestTrace t) {
        final int tId = t.getId();
        for (InvocationTrace i: t.getInvocations()) {
            csv(tInvocations,
                    tId,
                    i.getEntry(),
                    i.getExit(),
                    i.getDepth(),
                    i.getCallLine(),
                    i.getMethod().getId(),
                    i.getThisObjectId(),
                    i.isRet() ? 1 : 0,
                    i.getPrimitiveId(),
                    i.getValue(),
                    i.getReturnLine());
            locals(tId, i);
            modifications(tId, i);
            accesses(tId, i);
            exThrows(tId, i);
            exCatches(tId, i);
        }
    }

    private void locals(int tId, InvocationTrace i) {
        long iId = i.getEntry();
        for (LocalVarTrace lv: i.getLocals()) {
            csv(tLocals,
                    tId,
                    iId,
                    lv.getStep(),
                    lv.getVariable().getMethod().getId(),
                    lv.getVariable().getId(),
                    lv.getPrimitiveId(),
                    lv.getValue(),
                    lv.getLine());
        }
    }

    public void modifications(int tId, InvocationTrace i) {
        long iId = i.getEntry();
        for (FieldTrace f: i.getModifications()) {
            csv(tFields,
                    tId,
                    iId,
                    f.getStep(),
                    f.getObjectId(),
                    f.getField().getId(),
                    f.getPrimitiveId(),
                    f.getValue(),
                    f.getLine());
        }
    }

    public void accesses(int tId, InvocationTrace i) {
        long iId = i.getEntry();
        for (FieldAccessTrace f: i.getAccesses()) {
            csv(tAccesses,
                    tId,
                    iId,
                    f.getStep(),
                    f.getObjectId(),
                    f.getField().getId(),
                    f.getLine());
        }
    }

    private void exThrows(int tId, InvocationTrace i) {
        long iId = i.getEntry();
        for (ThrowTrace t: i.getExThrows()) {
            csv(tThrows,
                    tId,
                    iId,
                    t.getStep(),
                    t.getException().getId(),
                    t.getLine());
        }
    }

    private void exCatches(int tId, InvocationTrace i) {
        long iId = i.getEntry();
        for (CatchTrace t: i.getExCatches()) {
            csv(tCatches,
                    tId,
                    iId,
                    t.getStep(),
                    t.getException().getId(),
                    t.getLine());
        }
    }

}
