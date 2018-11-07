package de.hpi.accidit.js.parser;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class StringRule implements Rule {
    
    private final String string;
    private final boolean ignoreCase;

    protected StringRule(String string, boolean ignoreCase) {
        this.string = string;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public Node apply(ParserState parserState, int index) {
        String text = parserState.getInput().toString();
        int len = string.length();
        boolean match = false;
        if (index + len <= text.length()) {
            if (ignoreCase) {
                text = text.substring(index, index + len);
                match = string.equalsIgnoreCase(text);
            } else {
                int i = text.indexOf(string, index);
                match = i == index;
            }
        }
        if (match) {
            return new Node(parserState.getInput(index, index+len), "STRING");
        } else {
            return parserState.mismatch(toString(), index);
        }
    }

    @Override
    public String toString() {
        return "'" + string + "'";
    }
    
    private static final Map<String, StringRule> INSTANCES = new HashMap<>();
    
    public static StringRule get(String string, boolean ignoreCase) {
        return INSTANCES.computeIfAbsent(string + ignoreCase, k -> new StringRule(string, ignoreCase));
    }
}
