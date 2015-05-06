package de.hpi.accidit.db.csv;

import java.io.File;
import java.io.IOException;

public interface CsvFormat {
    
    CsvWriter writeToFile(File f) throws IOException;
    
}
