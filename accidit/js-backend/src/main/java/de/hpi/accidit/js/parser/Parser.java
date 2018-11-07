package de.hpi.accidit.js.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 *
 */
public class Parser {
    
    private final Map<String, Rule> rules = new HashMap<>();
    private boolean initialized = false;

    public Parser() {
    }
    
    protected void initialize() {
        addRule("WS", "/\\s+/");
        addRule("\u2423", "WS *", Hidden.as("WHITESPACE"));
        addRule("~", "WS +", Hidden.as("WHITESPACE"));
    }
    
    private void init() {
        if (initialized) return;
        initialized = true;
        initialize();
    }
    
    public Node parse(String text, String key) {
        init();
        return new ParserState(this, text).parse(key);
    }
    
    public void setRule(String key, String def) {
        setRule(key, Rule.parse(def, key));
    }
    
    public void setRule(String key, String def, UnaryOperator<Node> transformation) {
        setRule(key, Rule.parse(def, transformation));
    }
    
    public void setRule(String key, Rule rule) {
        rules.put(key, rule);
    }
    
    public void addRule(String key, UnaryOperator<Node> transformation, String... def) {
        for (String s: def) {
            addRule(key, Rule.parse(s, transformation));
        }
    }
    
    public void addRule(String key, String... def) {
        for (String s: def) {
            addRule(key, Rule.parse(s, key));
        }
    }
    
    public void addAlias(String key, String... def) {
        for (String s: def) {
            addRule(key, Rule.parse(s));
        }
    }
    
    public void addRule(String key, String def, UnaryOperator<Node> transformation) {
        addRule(key, Rule.parse(def, transformation).rename(key));
    }
    
    public void addRule(String key, Rule rule) {
        Rule r = rules.get(key);
        if (r == null) {
            setRule(key, rule);
            return;
        }
        
        if (!(r instanceof ChoiceRule)) {
            r = new ChoiceRule(new ArrayList<>(Arrays.asList(r)));
            setRule(key, r);
        }
        ChoiceRule cr = (ChoiceRule) r;
        cr.getOptions().add(rule);
    }

    public Rule getRule(String key) {
        Rule r = rules.get(key);
        if (r == null) throw new IllegalArgumentException(key);
        return r;
    }
    
}
