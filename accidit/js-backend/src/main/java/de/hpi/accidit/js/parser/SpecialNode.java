package de.hpi.accidit.js.parser;

import java.util.List;

/**
 *
 */
public class SpecialNode extends Node {

    public SpecialNode(Input input, String key) {
        super(input, key);
    }

    public SpecialNode(Node src, String key) {
        super(src, key);
    }

    public SpecialNode(Input input, String key, List<Node> children) {
        super(input, key, children);
    }

    @Override
    public Node rename(String newKey) {
        return this;
    }
}
