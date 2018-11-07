package de.hpi.accidit.js.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class RegexRule implements Rule {
    
    private final Pattern pattern;

    protected RegexRule(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public Node apply(ParserState parserState, int index) {
        Matcher m = pattern.matcher(parserState.getInput());
        m.region(index, m.regionEnd());
        if (m.lookingAt()) {
            return new Node(parserState.getInput(m.start(), m.end()), "PATTERN");
        }
        return parserState.mismatch(toString(), index);
    }

    @Override
    public String toString() {
        return "/" + pattern.toString() + "/";
    }
    
    private static final Map<String, RegexRule> INSTANCES = new HashMap<>();
    
    public static RegexRule get(String pattern, boolean ignoreCase) {
        return INSTANCES.computeIfAbsent(pattern + ignoreCase, k -> {
            Pattern p = Pattern.compile(pattern, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
            return new RegexRule(p);
        });
    }
}
