package de.hpi.accidit.js.sqlscript;

import de.hpi.accidit.js.parser.Input;
import de.hpi.accidit.js.parser.Node;
import de.hpi.accidit.js.parser.SpecialNode;
import java.util.List;

/**
 *
 */
public class Parameter extends SpecialNode {

    public Parameter(Input input, String key) {
        super(input, key);
    }

    public Parameter(Node src) {
        super(src, "Parameter");
    }

    public Parameter(Node src, String key) {
        super(src, key);
    }

    public Parameter(Input input, String key, List<Node> children) {
        super(input, key, children);
    }
    
    public boolean isOut() {
        return get(0).getValue().startsWith("OUT");
    }
    
    public Identifier getType() {
        return get("Identifier", 1);
    }
    
    public Identifier getName() {
        return get("Identifier", 0);
    }
}
