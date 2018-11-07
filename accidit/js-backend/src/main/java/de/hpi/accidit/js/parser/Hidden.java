package de.hpi.accidit.js.parser;

import java.util.function.UnaryOperator;

/**
 *
 */
public class Hidden extends Node {

    public Hidden(Node src) {
        super(src, src.getKey());
    }

    public Hidden(Node src, String key) {
        super(src, key);
    }

    @Override
    public boolean isHidden() {
        return true;
    }
    
    public static UnaryOperator<Node> as(String key) {
        return n -> new Hidden(n, key);
    }
}
