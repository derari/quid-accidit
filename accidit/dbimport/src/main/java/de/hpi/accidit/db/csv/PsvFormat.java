package de.hpi.accidit.db.csv;

import java.io.BufferedWriter;
import java.io.IOException;

public class PsvFormat extends AbstractCsvFormat {

    @Override
    protected void write(BufferedWriter out, String[] values) throws IOException {
        boolean first = true;
        for (String s: values) {
            if (first) first = false;
            else out.append('|');
            out.append(s);
        }
        out.append('\n');
    }
}
