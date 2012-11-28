package de.hpi.accidit.trace;

import de.hpi.accidit.model.PrimitiveType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class InvocationMatcher extends TypeSafeDiagnosingMatcher<InvocationTrace> {

    private String mName;
    
    private int depth = -1;
    
    private boolean returned = false;
    private PrimitiveType resultType = null;
    private long result = -1;

    public InvocationMatcher(String mName) {
        this.mName = mName;
    }
    
    @Override
    protected boolean matchesSafely(InvocationTrace t, Description d) {
        String actualName = t.getMethod().getName();
        if (!mName.equals(actualName)) {
            d.appendText(actualName);
            d.appendText("()");
            return false;
        }
        if (depth >= 0) {
            if (depth != t.getDepth()) {
                d.appendText("depth was " + t.getDepth());
                return false;
            }
        }
        if (returned) {
            if (!t.isReturned()) {
                d.appendText("threw an exception");
                return false;
            }
            if (result != t.getResult()) {
                d.appendText("returned " + t.getResult());
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void describeTo(Description d) {
        d.appendText(mName).appendText("()");
        if (depth >= 0) {
            d.appendText("[" + depth + "]");
        }
        if (returned) {
            d.appendText(" -> ");
            //d.appendText(String.valueOf(resultType));
            d.appendText(String.format(" %d", result));
        }
    }
    
    public InvocationMatcher depth(int d) {
        this.depth = d;
        return this;
    }
    
    public InvocationMatcher returning(long result) {
        returned = true;
        resultType = PrimitiveType.VOID;
        this.result = result;
        return this;        
    }
    
    public InvocationMatcher returningVoid() {
        returned = true;
        resultType = PrimitiveType.VOID;
        result = resultType.toValueId(null);
        return this;
    }
    
}
