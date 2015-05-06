package de.hpi.accidit.db.csv;

import java.io.IOException;

public interface CsvWriter extends AutoCloseable {

    void writeLine(String[] values) throws IOException;
    
    @Override
    void close() throws IOException;
}
