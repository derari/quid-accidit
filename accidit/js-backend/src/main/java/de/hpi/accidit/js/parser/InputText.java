package de.hpi.accidit.js.parser;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class InputText implements Input {

    private final String text;
    private int[] lineBreaks;

    public InputText(String text) {
        this.text = text;
        List<Integer> lbList = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') lbList.add(i);
        }
        lineBreaks = lbList.stream().mapToInt(i -> i).toArray();
    }
    
    protected int indexToLine(int i) {
        for (int l = 0; l < lineBreaks.length; l++) {
            if (lineBreaks[l] > i) return l + 1;
        }
        return lineBreaks.length;
    }
    
    @Override
    public Input subSequence(int start, int end) {
        return new SubInput(start, end);
    }

    @Override
    public int getStart() {
        return 0;
    }

    @Override
    public int getEnd() {
        return length();
    }

    @Override
    public int getStartLine() {
        return 1;
    }

    @Override
    public int[] getLineAndColumn(int index) {
        int line, col = index;
        for (line = 0; line < lineBreaks.length; line++) {
            if (lineBreaks[line] > index) break;
        }
        if (line > 0) {
            col -= lineBreaks[line-1];
        }
        return new int[]{line+1, col};
    }

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public char charAt(int index) {
        return text.charAt(index);
    }

    @Override
    public String toString() {
        return text;
    }

    protected class SubInput implements Input {
        private final int start, end;
        private final int startLine;

        public SubInput(int start, int end) {
            this.start = start;
            this.end = end;
            if (end < start) throw new IllegalArgumentException();
            this.startLine = indexToLine(start);
        }

        @Override
        public Input subSequence(int start, int end) {
            return InputText.this.subSequence(this.start+start, this.start+end);
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getEnd() {
            return end;
        }

        @Override
        public int getStartLine() {
            return startLine;
        }

        @Override
        public int[] getLineAndColumn(int index) {
            return InputText.this.getLineAndColumn(start + index);
        }

        @Override
        public int length() {
            return end - start;
        }

        @Override
        public char charAt(int index) {
            return InputText.this.charAt(start + index);
        }

        @Override
        public String toString() {
            return InputText.this.toString().substring(start, end);
        }
    }
}
