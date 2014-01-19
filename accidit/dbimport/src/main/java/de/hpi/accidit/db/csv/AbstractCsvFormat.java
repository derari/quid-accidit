package de.hpi.accidit.db.csv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public abstract class AbstractCsvFormat implements CsvFormat {

    @Override
    public CsvWriter writeToFile(File f) throws IOException {
        return new Writer(f);
    }
    
    protected abstract void write(BufferedWriter out, String[] values) throws IOException;
    
    private class Writer implements CsvWriter {

        private final BufferedWriter out;

        public Writer(File f) throws IOException {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "utf-8"));
        }
        
        @Override
        public void writeLine(String[] values) throws IOException {
            write(out, values);
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }
}
