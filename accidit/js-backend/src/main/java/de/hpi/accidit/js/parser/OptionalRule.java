package de.hpi.accidit.js.parser;

/**
 *
 */
public class OptionalRule implements Rule {
    
    private final Rule rule;

    public OptionalRule(Rule rule) {
        this.rule = rule;
    }

    @Override
    public Node apply(ParserState parserState, int index) {
        Node n = rule.get(parserState, index);
        if (n != null) return n;
        return new Node(parserState.getInput(index, index), "EMPTY");
    }

    @Override
    public String toString() {
        return rule + "?";
    }
}
