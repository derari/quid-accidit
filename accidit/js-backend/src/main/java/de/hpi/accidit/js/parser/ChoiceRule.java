package de.hpi.accidit.js.parser;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class ChoiceRule implements Rule {
    
    private final List<Rule> options;

    public ChoiceRule(List<Rule> options) {
        this.options = options;
    }

    public List<Rule> getOptions() {
        return options;
    }

    @Override
    public Node get(ParserState parserState, int index) {
        return apply(parserState, index);
    }

    @Override
    public Node apply(ParserState parserState, int index) {
        for (Rule r: options) {
            Node n = r.get(parserState, index);
            if (n != null) return n;
        }
        return null;
    }

    @Override
    public String toString() {
        return options.stream().map(Object::toString).collect(Collectors.joining(" / "));
    }
}
