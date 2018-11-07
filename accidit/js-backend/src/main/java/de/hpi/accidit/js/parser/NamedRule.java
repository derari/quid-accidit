package de.hpi.accidit.js.parser;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class NamedRule implements Rule {
    
    private final String name;

    public NamedRule(String name) {
        this.name = name;
    }

    @Override
    public Node get(ParserState parserState, int index) {
        return parserState.getParser().getRule(name).get(parserState, index);
    }

    @Override
    public Node apply(ParserState parserState, int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String toString() {
        return name;
    }
    
    private static final Map<String, NamedRule> INSTANCES = new HashMap<>();
    
    public static NamedRule get(String name) {
        return INSTANCES.computeIfAbsent(name, NamedRule::new);
    }
}
