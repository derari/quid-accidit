package de.hpi.accidit.js.parser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ListRule implements Rule {
    
    private final Rule item;
    private final Rule separator;

    public ListRule(Rule item, Rule separator) {
        this.item = item;
        this.separator = separator.transform(Hidden.as("SEP"));
    }

    @Override
    public Node get(ParserState parserState, int index) {
        return apply(parserState, index);
    }

    @Override
    public Node apply(ParserState parserState, int index) {
        List<Node> children = new ArrayList<>();
        Node n = item.get(parserState, index);
        if (n == null) return null;
        children.add(n);
        while (true) {
            Node s = separator.get(parserState, n.getEnd());
            if (s == null) break;
            Node n2 = item.get(parserState, s.getEnd());
            if (n2 == null) break;
            n = n2;
            children.add(s);
            children.add(n);
        }
        return new Node(parserState.getInput(index, n.getEnd()), "LIST", children);
    }

    @Override
    public String toString() {
        return item + "{" + separator + "}";
    }
}
