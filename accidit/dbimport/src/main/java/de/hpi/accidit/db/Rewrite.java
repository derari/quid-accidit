package de.hpi.accidit.db;

import de.hpi.accidit.db.csv.CsvFormat;
import de.hpi.accidit.db.csv.CsvReader;
import de.hpi.accidit.db.csv.CsvWriter;
import java.io.File;
import java.io.IOException;

public class Rewrite {
    
    private final Model model;
    private final CsvFormat outFormat;
    private final String inputDir;
    private final String outputDir;

    public Rewrite(Model model, CsvFormat outFormat, String inputDir, String outputDir) {
        this.model = model;
        this.outFormat = outFormat;
        this.inputDir = inputDir;
        this.outputDir = outputDir;
    }

    public void run() throws Exception {
        rewriteTypes();
    }

    private void rewriteTypes() throws IOException {
        File fIn = new File(inputDir, "mTypes.csv");
        File fOut = new File(outputDir, "mTypes.csv");
        String[] row = {};
        try (CsvReader in = new CsvReader(fIn);
             CsvWriter out = outFormat.writeToFile(fOut)) {
            while ((row = in.nextRow(row)) != null) {
                int id = Integer.parseInt(row[0]);
                String name = row[1];
                id = model.lookupType(id, name);
                row[0] = Integer.toString(id);
                out.writeLine(row);
            }
        }
    }
}
