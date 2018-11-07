package de.hpi.accidit.js.parser;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class Node {
    
    private final Input input;
    private final String key;
    private final List<Node> children;

    public Node(Input input, String key) {
        this(input, key, Collections.emptyList());
    }
    
    public Node(Node src, String key) {
        this(src.getMatch(), key, src.getAllChildren());
    }

    public Node(Input input, String key, List<Node> children) {
        this.input = input;
        this.key = key;
        this.children = children;
    }

    public Node rename(String newKey) {
        return new Node(this, newKey);
    }

    public Input getMatch() {
        return input;
    }
    
    public String getValue() {
        return getMatch().toString();
    }

    public int getStart() {
        return input.getStart();
    }

    public int getEnd() {
        return input.getEnd();
    }
    
    public int getStartLine() {
        return input.getStartLine();
    }

    public String getKey() {
        return key;
    }
    
    public boolean isHidden() {
        return false;
    }
    
    public Node get(int i) {
        return children.get(i);
    }
    
    public <N extends Node> N get(String key) {
        return (N) get(key, 0);
    }
    
    public <N extends Node> N get(String key, int index) {
        int i = index;
        for (Node n: children) {
            if (n.getKey().equals(key) && i-- == 0) {
                return (N) n;
            }
        }
        if (i == index) {
            throw new IllegalArgumentException(key);
        } else {
            throw new IndexOutOfBoundsException(key + ": " + index + "/" + (index-i));
        }
    }
    
    public boolean has(String key) {
        for (Node n: children) {
            if (n.getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }
    
    public int count(String key) {
        return (int) stream(key).count();
    }

    public List<Node> getAllChildren() {
        return children;
    }
    
    public List<Node> getChildren() {
        return stream().collect(Collectors.toList());
    }
    
    public <N extends Node> List<N> getAll(String key) {
        return this.<N>stream(key).collect(Collectors.toList());
    }
    
    public <N extends Node> Stream<N> stream() {
        return (Stream) children.stream()
                .filter(n -> !n.isHidden());
    }
    
    public <N extends Node> Stream<N> stream(String key) {
        return (Stream) children.stream().filter(n -> n.getKey().equals(key));
    }
    
    protected void print(PrintWriter pw, int indent) {
        if (isHidden()) return;
        for (int i = 0; i < indent; i++) pw.print(" ");
        pw.print(getKey());
        if (children.isEmpty()) {
            pw.print(" ");
            pw.println(getValue().replace('\n', '\u21B5'));
        } else {
            pw.println();
            children.forEach(n -> n.print(pw, indent+1));
        }
    }

    public void printTo(OutputStream ps) {
        printTo(new PrintWriter(ps));
    }

    public void printTo(PrintWriter pw) {
        print(pw, 0);
        pw.flush();
    }
    
    @Override
    public String toString() {
        String value = getValue().replace('\n', '\u21B5');
        if (value.length() > 20) {
            value = value.substring(0, 15) + 
                    "\u2026" + 
                    value.substring(value.length()-5, value.length());
        }
        return getKey() +
                (children.isEmpty() ? "" : "[" + children.size() + "]") +
                " " + value;
    }
}
