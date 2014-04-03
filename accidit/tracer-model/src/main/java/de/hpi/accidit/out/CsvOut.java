package de.hpi.accidit.out;

import de.hpi.accidit.model.*;
import de.hpi.accidit.out.internal.AccBufferedOutputStream;
import de.hpi.accidit.out.internal.AccPrintStream;
import de.hpi.accidit.trace.*;
import java.io.*;

/**
 *
 * @author Arian Treffer
 */
public class CsvOut implements Out {
    
    public static final class Csv {
        final AccBufferedOutputStream os;
        final AccPrintStream ps;
        boolean newLine = true;

        public Csv(File dir, String name) throws IOException {
//            Path p = Paths.get(new File(dir, name+".csv").toURI());
//            BufferedWriter bw = Files.newBufferedWriter(p, Charset.forName("utf-8"));
//            PrintWriter pw = new PrintWriter(pw)
            this.os = new AccBufferedOutputStream(
                    new FileOutputStream(new File(dir, name+".csv")),
                    1024 * 1024 * 16);
            this.ps = new AccPrintStream(os);
        }
        
        protected void write(String s) {
            ps.print(s);
        }
        
//        protected void write(char c) throws IOException {
//            
//            os.write(c);
//        }
        
        private void sep() {
            if (newLine) newLine = false;
            else ps.print(';');
        }
        
        public void p(Object value) {
            sep();
            if (value == null) {
                write("NULL");
            } else {
                ps.print(value);
            }
        }

        public void px(Object value) {
            sep();
            if (value == null) {
                write("NULL");
            } else {
                ps.print('"');
                ps.print(value);
                ps.print('"');
            }
        }

        public void p(boolean value) {
            p(value ? 1 : 0);
        }
        
        public void pUnsigned(int value) {
            sep();
            if (value < 0) {
                write("NULL");
            } else {
                ps.print(value);
            }
        }
        
        public void pUnsigned(long value) {
            sep();
            if (value < 0) {
                write("NULL");
            } else {
                ps.print(value);
            }
        }
        
        public void p(int value) {
            sep();
            ps.print(value);
        }

    
        public void p(long value) {
            sep();
            ps.print(value);
        }
        
        public void pValue(ValueTrace value) {
            sep();
            ps.print(value.getPrimType().getKey());
            sep();
            ps.print(value.getValueId());
        }

        public void nl() {
            ps.print('\n');
            newLine = true;
        }
        
        public void flush() {
            ps.flush();
        }
    }

    private final Csv mType;
    private final Csv mExtends;
    private final Csv mMethod;
    private final Csv mVariable;
    private final Csv mField;
    private final Csv tTrace;
    private final Csv tObject;
    private final Csv tCall;
    private final Csv tExit;
    private final Csv tThrow;
    private final Csv tCatch;
    private final Csv tVariable;
    private final Csv tPut;
    private final Csv tGet;
    private final Csv tArrayPut;
    private final Csv tArrayGet;

    public CsvOut(File dir) throws IOException {
        dir.mkdirs();
        this.mType = new Csv(dir, "mType");
        this.mExtends = new Csv(dir, "mExtends");
        this.mMethod = new Csv(dir, "mMethod");
        this.mVariable = new Csv(dir, "mVariable");
        this.mField = new Csv(dir, "mField");
        this.tTrace = new Csv(dir, "tTrace");
        this.tObject = new Csv(dir, "tObject");
        this.tCall = new Csv(dir, "tCall");
        this.tExit = new Csv(dir, "tExit");
        this.tThrow = new Csv(dir, "tThrow");
        this.tCatch = new Csv(dir, "tCatch");
        this.tVariable = new Csv(dir, "tVariable");
        this.tPut = new Csv(dir, "tPut");
        this.tGet = new Csv(dir, "tGet");
        this.tArrayPut = new Csv(dir, "tArrayPut");
        this.tArrayGet = new Csv(dir, "tArrayGet");
    }
    
    @Override
    public void end(ThreadTrace trace) {
        mType.flush();
        mExtends.flush();
        mMethod.flush();
        mVariable.flush();
        mField.flush();
        tTrace.flush();
        tObject.flush();
        tCall.flush();
        tExit.flush();
        tThrow.flush();
        tCatch.flush();
        tVariable.flush();
        tPut.flush();
        tGet.flush();
        tArrayPut.flush();
        tArrayGet.flush();
    }
    
    private Integer tId(TypeDescriptor type) {
        return type != null ? type.getModelId() : null;
    }
    
    private Long oId(ObjectTrace obj) {
        return obj != null ? obj.getId() : null;
    }
    
    @Override
    public void type(TypeDescriptor type) {
        mType.p(type.getModelId());
        mType.p(type.getName());
        mType.p(type.getSource());
        mType.p(tId(type.getComponentType()));
        mType.nl();
        for (TypeDescriptor sup: type.getSupers()) {
            ext(type, sup);
        }
    }
    
    private void ext(TypeDescriptor type, TypeDescriptor sup) {
        mExtends.p(type.getModelId());
        mExtends.p(sup.getModelId());
        mExtends.nl();
    }

    @Override
    public void method(MethodDescriptor method) {
        mMethod.p(method.getModelId());
        mMethod.p(method.getOwner().getModelId());
        mMethod.p(method.getName());
        mMethod.px(method.getDescriptor());
        mMethod.pUnsigned(method.getLine());
        mMethod.p(method.getHashcode());
        mMethod.nl();
    }

    @Override
    public void variable(VarDescriptor var) {
        mVariable.p(var.getMethod().getModelId());
        mVariable.p(var.getId());
        mVariable.p(var.getName());
        mVariable.p(var.getType().getModelId());
        mVariable.p(var.isArgument());
        mVariable.nl();
    }

    @Override
    public void field(FieldDescriptor field) {
        mField.p(field.getModelId());
        mField.p(field.getOwner().getModelId());
        mField.p(field.getName());
        mField.p(field.getType().getModelId());
        mField.nl();
    }
    
    @Override
    public void begin(ThreadTrace trace) {
        tTrace.p(trace.getId());
        tTrace.p(trace.getName());
        tTrace.nl();
    }

    @Override
    public void traceObject(ThreadTrace trace, ObjectTrace object) {
        tObject.p(trace.getId());
        tObject.p(object.getId());
        tObject.p(object.getType().getModelId());
        int len = object.getArrayLength();
        if (len < 0) tObject.p(null);
        else tObject.p(len);
        tObject.nl();
    }
    
    @Override
    public void traceCall(CallTrace call) {
        tCall.p(call.getTrace().getId());
        tCall.pUnsigned(call.getCallStep());
        tCall.p(call.getStep());
        tCall.p(call.getExitStep());
        tCall.p(call.getMethod().getModelId());
        tCall.p(oId(call.getInstance()));
        tCall.p(call.getDepth());
        tCall.p(call.getLine());
        tCall.nl();
    }

    @Override
    public void traceExit(CallTrace call, ExitTrace exit) {
        tExit.p(call.getTrace().getId());
//        tExit.p(call.getStep());
        tExit.p(exit.getStep());
        tExit.p(exit.isReturned());
        tExit.pValue(exit);
        tExit.p(exit.getLine());
        tExit.nl();
    }

    @Override
    public void traceThrow(CallTrace call, ThrowableTrace ex) {
        tThrow.p(call.getTrace().getId());
        tThrow.p(call.getStep());
        tThrow.p(ex.getStep());
        tThrow.p(ex.getThrowable().getId());
        tThrow.p(ex.getLine());
        tThrow.nl();
    }

    @Override
    public void traceCatch(CallTrace call, ThrowableTrace ex) {
        tCatch.p(call.getTrace().getId());
        tCatch.p(call.getStep());
        tCatch.p(ex.getStep());
        tCatch.p(ex.getThrowable().getId());
        tCatch.p(ex.getLine());
        tCatch.nl();
    }

    @Override
    public void traceVariable(CallTrace call, VariableTrace var) {
        tVariable.p(call.getTrace().getId());
        tVariable.p(call.getStep());
        tVariable.p(var.getStep());
        tVariable.p(var.getVaribale().getMethod().getModelId());
        tVariable.p(var.getVaribale().getId());
        tVariable.pValue(var);
        tVariable.p(var.getLine());
        tVariable.nl();
    }

    @Override
    public void traceGet(CallTrace call, FieldTrace field) {
        tGet.p(call.getTrace().getId());
        tGet.p(call.getStep());
        tGet.p(field.getStep());
        tGet.p(oId(field.getInstance()));
        tGet.p(field.getField().getModelId());
        tGet.pValue(field);
        tGet.p(field.getLine());
        tGet.nl();
    }

    @Override
    public void tracePut(CallTrace call, FieldTrace field) {
        tPut.p(call.getTrace().getId());
        tPut.p(call.getStep());
        tPut.p(field.getStep());
        tPut.p(oId(field.getInstance()));
        tPut.p(field.getField().getModelId());
        tPut.pValue(field);
        tPut.p(field.getLine());
        tPut.nl();
    }

    @Override
    public void traceArrayGet(CallTrace call, ArrayItemTrace array) {
        tArrayGet.p(call.getTrace().getId());
        tArrayGet.p(call.getStep());
        tArrayGet.p(array.getStep());
        tArrayGet.p(array.getInstance().getId());
        tArrayGet.p(array.getIndex());
        tArrayGet.pValue(array);
        tArrayGet.p(array.getLine());
        tArrayGet.nl();
    }

    @Override
    public void traceArrayPut(CallTrace call, ArrayItemTrace array) {
        tArrayPut.p(call.getTrace().getId());
        tArrayPut.p(call.getStep());
        tArrayPut.p(array.getStep());
        tArrayPut.p(array.getInstance().getId());
        tArrayPut.p(array.getIndex());
        tArrayPut.pValue(array);
        tArrayPut.p(array.getLine());
        tArrayPut.nl();
    }

}
