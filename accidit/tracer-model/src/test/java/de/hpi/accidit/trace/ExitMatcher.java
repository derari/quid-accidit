package de.hpi.accidit.trace;

import de.hpi.accidit.model.PrimitiveType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class ExitMatcher extends TypeSafeDiagnosingMatcher<ExitTrace> {
    
    private boolean returned = false;
    private PrimitiveType resultType = null;
    private long result = -1;

    public ExitMatcher() {
    }
    
    @Override
    protected boolean matchesSafely(ExitTrace t, Description d) {
        if (returned) {
            if (!t.isReturned()) {
                d.appendText("threw an exception");
                return false;
            }
            if (result != t.getValueId()) {
                d.appendText("returned " + t.getValueId());
                return false;
            }
        } else {
            if (t.isReturned()) {
                d.appendText("returned");
                return false;
            }
            if (result != t.getValueId()) {
                d.appendText("threw " + t.getValueId());
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void describeTo(Description d) {
        if (returned) {
            d.appendText(" -> ");
            //d.appendText(String.valueOf(resultType));
            d.appendText(String.format(" %d", result));
        }
    }
    
    public ExitMatcher returning(long result) {
        returned = true;
        resultType = PrimitiveType.VOID;
        this.result = result;
        return this;        
    }
    
    public ExitMatcher failing(long ex) {
        returned = false;
        resultType = PrimitiveType.VOID;
        this.result = ex;
        return this;  
    }
    
    public ExitMatcher returningVoid() {
        returned = true;
        resultType = PrimitiveType.VOID;
        result = resultType.toValueId(null);
        return this;
    }
    
}
