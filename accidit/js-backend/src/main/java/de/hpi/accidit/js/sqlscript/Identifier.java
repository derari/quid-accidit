package de.hpi.accidit.js.sqlscript;

import de.hpi.accidit.js.parser.Input;
import de.hpi.accidit.js.parser.Node;
import de.hpi.accidit.js.parser.SpecialNode;
import java.util.List;

/**
 *
 */
public class Identifier extends SpecialNode {

    public Identifier(Input input, String key) {
        super(input, key);
    }

    public Identifier(Node src) {
        super(src, "Identifier");
    }

    public Identifier(Node src, String key) {
        super(src, key);
    }

    public Identifier(Input input, String key, List<Node> children) {
        super(input, key, children);
    }
    
    public String getWithPrefix(String prefix) {
        String val = getValue();
        if (val.endsWith("\"")) {
            int i = val.substring(0, val.length()-1).lastIndexOf('"') + 1;
            return val.substring(0, i) + prefix + val.substring(i);
        } else {
            int i = val.indexOf('.');
            if (i < 0) return prefix + val;
            return val.substring(0, i+1) + prefix + val.substring(i+1);
        }
    }
    
    public String getWithFixes(String prefix, String suffix) {
        String val = getWithPrefix(prefix);
        if (val.endsWith("\"")) {
            return val.substring(0, val.length()-1) + suffix + '"';
        } else {
            return val + suffix;
        }
    }
    
}
