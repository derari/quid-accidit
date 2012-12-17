package de.hpi.accidit.trace;

import de.hpi.accidit.model.PrimitiveType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class CallMatcher extends TypeSafeDiagnosingMatcher<CallTrace> {

    private String mName;
    
    private int depth = -1;
    
    public CallMatcher(String mName) {
        this.mName = mName;
    }
    
    @Override
    protected boolean matchesSafely(CallTrace t, Description d) {
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
        return true;
    }

    @Override
    public void describeTo(Description d) {
        d.appendText(mName).appendText("()");
        if (depth >= 0) {
            d.appendText("[" + depth + "]");
        }
    }
    
    public CallMatcher depth(int d) {
        this.depth = d;
        return this;
    }
    
}
