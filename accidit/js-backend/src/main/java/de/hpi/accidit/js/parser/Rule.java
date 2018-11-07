package de.hpi.accidit.js.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public interface Rule {

    default Node get(ParserState parserState, int index) {
        return parserState.get(this, index);
    }
    
    Node apply(ParserState parserState, int index);
    
    default Rule list(Rule separator) {
        return new ListRule(this, separator);
    }
    
    default Rule transform(UnaryOperator<Node> transformation) {
        if (transformation == UnaryOperator.<Node>identity()) return this;
        class TransformationRule implements Rule {
            @Override
            public Node apply(ParserState parserState, int index) {
                Node n = Rule.this.get(parserState, index);
                return n != null ? transformation.apply(n) : null;
            }
            @Override
            public String toString() {
                return Rule.this.toString() + "´";
            }
        }
        return new TransformationRule();
    }
    
    default Rule rename(String newKey) {
        if (newKey == null) return this;
        class RenameRule implements Rule {
            @Override
            public Rule rename(String newKey) {
                return Rule.this.rename(newKey);
            }
            @Override
            public Node apply(ParserState parserState, int index) {
                Node n = Rule.this.get(parserState, index);
                if (n != null) {
                    return n.rename(newKey);
                } else {
                    String other = Rule.this.toString();
                    while (other.endsWith("´")) other = other.substring(0, other.length()-1);
                    return parserState.replaceMismatch(other, newKey, index);
                }
            }
            @Override
            public String toString() {
                return newKey + ": " + Rule.this.toString();
            }
        }
        return new RenameRule();
    }
    
    default Rule hidden() {
        return transform(Hidden::new);
    }
    
    default Rule hidden(String newKey) {
        if (newKey == null) return this;
        return transform(n -> new Hidden(n, newKey));
    }
    
    default Rule repeat() {
        return new ListRule(this, EMPTY);
    }
    
    default Rule optional() {
        return new OptionalRule(this);
    }
    
    static Rule parse(String definition, String name) {
        return parse(definition).rename(name);
    }
    
    static Rule parse(String definition, UnaryOperator<Node> transformation) {
        return parse(definition).transform(transformation);
    }
    
    static Rule parse(String definition) {
        Matcher m = RULE_SYNTAX.matcher(definition);
        List<Rule> options = null;
        List<Rule> steps = new ArrayList<>();
        while (m.lookingAt()) {
            if (m.group(1) != null) {
                String s = m.group(1).trim();
                if (!s.equals("_")) {
                    steps.add(NamedRule.get(s));
                }
            } else if (m.group(2) != null) {
                final String QUOTE = "'";
                String s = m.group(2);
                String flags = s.substring(s.lastIndexOf(QUOTE));
                boolean ignoreCase = flags.contains("i");
                s = s.substring(1, s.lastIndexOf(QUOTE)).replace(QUOTE+QUOTE, QUOTE);
                steps.add(StringRule.get(s, ignoreCase));
            } else if (m.group(3) != null) {
                final String QUOTE = "/";
                String s = m.group(3);
                String flags = s.substring(s.lastIndexOf(QUOTE));
                boolean ignoreCase = flags.contains("i");
                s = s.substring(1, s.lastIndexOf(QUOTE)).replace(QUOTE+QUOTE, QUOTE);
                steps.add(RegexRule.get(s, ignoreCase));
            } else if (m.group(4) != null) {
                Rule seq = SequenceRule.get(steps);
                if (options == null) options = new ArrayList<>();
                options.add(seq);
                steps = new ArrayList<>();
            } else if (m.group(5) != null) {
                String s = m.group(5);
                steps.add(Rule.parse(s.substring(1, s.length()-1)));
            } else if (m.group(6) != null) {
                String s = m.group(6);
                Rule r = steps.remove(steps.size()-1);
                Rule sep = Rule.parse(s.substring(s.indexOf('{')+1, s.length()-1), Hidden.as("SEP"));
                r = r.list(sep);
                steps.add(r);
            } else if (m.group(7) != null) {
                Rule r = steps.remove(steps.size()-1);
                switch (m.group(7).trim()) {
                    case "*":
                        r = r.repeat().optional();
                        break;
                    case "+":
                        r = r.repeat();
                        break;
                    case "?":
                        r = r.optional();
                        break;
                    default:
                        throw new IllegalArgumentException(m.group());
                }
                steps.add(r);
            } else if (m.group(8) != null) {
                steps.add(new NamedRule("\u2423"));
            } else {
                throw new IllegalArgumentException(m.group());
            }
            m.region(m.end(), m.regionEnd());
        }
        
        if (m.regionStart() < m.regionEnd()) {
            throw new IllegalArgumentException(
                    definition.substring(0, m.regionStart()) +
                    "\u2021" +
                    definition.substring(m.regionStart()));
        }
        
        if (options != null) {
            Rule seq = SequenceRule.get(steps);
            options.add(seq);
            return new ChoiceRule(options);
        }
        
        if (steps.size() == 1) {
            return steps.get(0);
        }
        
        return SequenceRule.get(steps);
    }
    
    static final Pattern RULE_SYNTAX = Pattern.compile(
            "(\\s*[_~]\\s*|[-\\w]+)|('(?:[^']|'')*'i?)|(/(?:[^/]|//)*/(?:i?))|(\\|)|(\\(.+?\\))|(\\s*\\{.+\\})|(\\s*[+*?])|(\\s+)");

    static final Rule EMPTY = new StringRule("", false);
}
