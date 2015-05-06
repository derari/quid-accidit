package de.hpi.accidit.db.csv;

import java.io.*;
import java.util.Arrays;

public class CsvReader implements AutoCloseable {

    private final BufferedReader in;
    
    public CsvReader(File f) throws IOException {
        in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
    }
    
    public String[] nextRow(String[] array) throws IOException {
        final String line = in.readLine();
        if (line == null) return null;
        final int length = line.length();
        int columns = 0;
        int i = 0;
        while (i < length) {
            int start = start(line, i, length);
            int end;
            if (start != i) {
                end = endQuoted(line, start, length);
                i = end+2;
            } else {
                end = end(line, start, length);
                i = end+1;
            }
            String value = line.substring(start, end);
            array = addColumn(array, value, columns);
            columns++;
        }
        return array;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    private int start(String line, int i, int length) {
        char c = line.charAt(i);
        if (c == '"') {
            i++;
        }
        return i;
    }

    private int endQuoted(String line, int i, int length) {
        char c = line.charAt(i);
        while (c != '"') {
            if (c == '\\') i++;
            i++;
            c = line.charAt(i);
        }
        return i;
    }

    private int end(String line, int i, int length) {
        while (i < length && line.charAt(i) != ';') i++;
        return i;
    }

    private String[] addColumn(String[] array, String value, int i) {
        if (i >= array.length) {
            array = Arrays.copyOf(array, i+1);
        }
        if (value.equals("NULL")) value = null;
        array[i] = value;
        return array;
    }

}
