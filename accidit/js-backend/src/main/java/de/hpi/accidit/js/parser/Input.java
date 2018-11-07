package de.hpi.accidit.js.parser;

/**
 *
 */
public interface Input extends CharSequence {

    @Override
    Input subSequence(int start, int end);

    int getStart();

    int getEnd();

    int getStartLine();
    
    int[] getLineAndColumn(int index);
}
