package de.hpi.accidit.js.parser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SequenceRule implements Rule {
    
    private final List<Rule> steps;

    public SequenceRule(List<Rule> steps) {
        this.steps = steps;
    }

    @Override
    public Node get(ParserState parserState, int index) {
        return apply(parserState, index);
    }

    @Override
    public Node apply(ParserState parserState, int index) {
        List<Node> matches = new ArrayList<>();
        int i = index;
        for (Rule r: steps) {
            Node n = r.get(parserState, i);
            if (n == null) {
//                parserState.mismatch(r, i);
                return null;
            }
            matches.add(n);
            i = n.getEnd();
        }
        return new Node(parserState.getInput(index, i), "SEQUENCE", matches);
    }

    @Override
    public String toString() {
        if (steps.isEmpty()) return "''";
        return steps.get(0).toString() + "\u2026";
    }
    
    public static Rule get(List<Rule> rules) {
        if (rules.size() == 1) return rules.get(0);
        return new SequenceRule(rules);
    }
}
