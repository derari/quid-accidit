package de.hpi.accidit.js.sqlscript;

import de.hpi.accidit.js.parser.Input;
import de.hpi.accidit.js.parser.Node;
import de.hpi.accidit.js.parser.SpecialNode;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class Parameters extends SpecialNode {

    public Parameters(Input input, String key) {
        super(input, key);
    }

    public Parameters(Node src) {
        super(src, "Parameters");
    }

    public Parameters(Node src, String key) {
        super(src, key);
    }

    public Parameters(Input input, String key, List<Node> children) {
        super(input, key, children);
    }
    
    public List<String> getParameterNames() {
        return this.<Parameter>stream("Parameter")
                .map(p -> p.getName().getValue())
                .collect(Collectors.toList());
    }
}
