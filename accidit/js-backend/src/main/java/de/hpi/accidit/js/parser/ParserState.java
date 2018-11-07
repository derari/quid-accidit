package de.hpi.accidit.js.parser;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class ParserState {

    private final Parser parser;
    private final Input input;
    private final ConcurrentMap<Rule, Map<Integer, Node>> matches = new ConcurrentHashMap<>();
    
    private int lastMismatch = 0;
    private final Set<String> mismatches = new LinkedHashSet<>();

    public ParserState(Parser parser, String text) {
        this.parser = parser;
        this.input = new InputText(text);
    }

    public Parser getParser() {
        return parser;
    }

    public Input getInput() {
        return input;
    }
    
    public Input getInput(int start) {
        return input.subSequence(start, input.getEnd());
    }
    
    public Input getInput(int start, int end) {
        return input.subSequence(start, end);
    }
    
    public Node parse(String rule) {
        Node n = parse(rule, 0);
        if (n == null) {
            String t;
            if (lastMismatch < 10) {
                t = getInput().subSequence(0, lastMismatch).toString();
            } else {
                t = "\u2026" + getInput(lastMismatch-10, lastMismatch);
            }
            t += "\u2021";
            if (getInput().length() - lastMismatch < 10) {
                t += getInput(lastMismatch);
            } else {
                t += getInput(lastMismatch, lastMismatch + 10) + "\u2026";
            }
            int[] lc = input.getLineAndColumn(lastMismatch);
            throw new IllegalArgumentException(
                    "Error at " + lc[0] + ":" + lc[1] + 
                    ", expected " + mismatches +
                    ", got '" + t + "'");
        }
        return n;
    }
    
//    public Node mismatch(Rule rule, int index) {
//        if (rule instanceof NamedRule) {
//            // don't list
//        } else if (rule instanceof ChoiceRule) {
//            ((ChoiceRule) rule).getOptions().forEach(r -> mismatch(r, index));
//        } else {
//            mismatch(rule.toString(), index);
//        }
//        return null;
//    }
    
    public Node mismatch(String rule, int index) {
        if (lastMismatch < index) {
            lastMismatch = index;
            mismatches.clear();
        } else if (lastMismatch > index) {
            return null;
        }
        mismatches.add(rule);
        return null;
    }
    
    public Node replaceMismatch(String other, String actual, int index) {
        if (lastMismatch == index) {
            mismatches.remove(other);
        }
        return mismatch(actual, index);
    }
    
    public Node parse(String rule, int index) {
        return NamedRule.get(rule).get(this, index);
    }
    
    public Node get(Rule rule, int index) {
        Node n = ruleMatches(rule).computeIfAbsent(index, i -> applyRule(rule, index));
        return n != NO_MATCH ? n : null;
    }
    
    private Map<Integer, Node> ruleMatches(Rule rule) {
        return matches.computeIfAbsent(rule, r -> new HashMap<>());
    }
    
    private Node applyRule(Rule rule, int index) {
        try {
            Node n = rule.apply(this, index);
            if (n == null) {
//                throw new IllegalArgumentException("Return `mismatch` expected");
//                mismatch(key, index);
                return NO_MATCH;
            }
            return n;
        } catch (Exception e) {
            throw new IllegalStateException(rule + "[" + index + "]", e);
        }
    }
    
    private static final Node NO_MATCH = new Node(new InputText(""), "") {
        @Override public boolean isHidden() { return true; }
    };
}
