package de.hpi.accidit.js;

import de.hpi.accidit.js.parser.Node;
import de.hpi.accidit.js.sqlscript.HanaSqlScriptParser;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 *
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String file = args.length >= 1 ? args[0] : "proc.txt";
        String content = Files.lines(Paths.get(file)).collect(Collectors.joining("\n"));
        
        HanaSqlScriptParser parser = new HanaSqlScriptParser();
        Node proc = parser.parse(content);
        
        String out = args.length >= 2 ? args[1] : "out.sql";
        new TraceWriter(proc, new PrintStream(out)).run();
    }
}
