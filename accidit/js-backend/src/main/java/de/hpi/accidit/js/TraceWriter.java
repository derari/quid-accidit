package de.hpi.accidit.js;

import de.hpi.accidit.js.parser.Node;
import de.hpi.accidit.js.sqlscript.Identifier;
import de.hpi.accidit.js.sqlscript.Parameter;
import de.hpi.accidit.js.sqlscript.Parameters;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class TraceWriter {
    
    private static final String TRACE_SUFFIX = "_T__";
    private static final String ENTRY_SUFFIX = "_TE_";
    
    private static final String T_Trace = "Quid_Accidit.Traces";
    private static final String T_Step = "Quid_Accidit.Steps";
    private static final String T_Query = "Quid_Accidit.Queries";
    private static final String T_QueryArg = "Quid_Accidit.Query_Args";
    private static final String F_Snapshot = "Quid_Accidit.Snapshot";
    
    private static final String V_TraceId = "__t_id";
    private static final String V_Step = "__s_id";
    private static final String V_Entry_Step = "__e_id";
    private static final String V_Call_Entry_Step = "__ce_id";
    private static final String V_TimeStamp = "__ts";
    private static final String V_Count = "__count";

    private final Node procedure;
    private final Identifier id;
    private final StringWriter main, headers, postDeclare;
    private final PrintWriter pMain, pHeaders, pPostDeclare;
    private boolean postDeclareMode = false;
    private int incSteps = 0;
    
    private final Map<String, String> scalars = new HashMap<>();
    private final Map<String, Map<Integer, String>> queries = new HashMap<>();
            
    private final PrintStream out;

    public TraceWriter(Node procedure, PrintStream out) {
        this.procedure = procedure;
        this.out = out;
        id = procedure.get("Identifier");
        main = new StringWriter();
        pMain = new PrintWriter(main);
        headers = new StringWriter();
        pHeaders = new PrintWriter(headers);
        postDeclare = new StringWriter();
        pPostDeclare = new PrintWriter(postDeclare);
    }
    
    public void run() {
        m_print("-- MAIN --\n\n\n");
        writeTracedProcedure();
        writeTraceEntry();
        writeVariableViews();
        
        pHeaders.flush();
        pMain.flush();
        out.println(headers.toString());
        out.println(main.toString());
    }
    
    protected PrintWriter m_print(String s) {
        pMain.print(s);
        return pMain;
    }
    
    protected PrintWriter m_print(String s, Object... args) {
        pMain.printf(s, args);
        return pMain;
    }
    
    protected PrintWriter print(String s) {
        if (postDeclareMode) {
            pPostDeclare.print(s);
            return pPostDeclare;
        } else {
            return m_print(s);
        }
    }
    
    protected PrintWriter print(String s, Object... args) {
        if (postDeclareMode) {
            pPostDeclare.printf(s, args);
            return pPostDeclare;
        } else {
            return m_print(s, args);
        }
    }
    
    protected void declareMode(boolean declareMode) {
        if (declareMode) {
            enterDeclareBlock();
        } else {
            exitDeclareBlock();
        }
    }
    
    protected void enterDeclareBlock() {
        postDeclareMode = true;
    }
    
    protected void exitDeclareBlock() {
        if (postDeclareMode) {
            postDeclareMode = false;
            pPostDeclare.flush();
            pMain.print(postDeclare.toString());
            postDeclare.getBuffer().setLength(0);
            if (incSteps > 0) {
                incSteps--;
                incStep();
            }
        }
    }
    
    protected void declareProcedure(String name, String parameters, String moreParameters) {
        if (moreParameters.trim().isEmpty()) {
            declareProcedure(name, parameters);
        } else {
            declareProcedure(name, parameters + ", " + moreParameters);
        }
    }
    
    protected void declareProcedure(String name, String parameters) {
//        pHeaders.printf("DROP PROCEDURE %s;\n", name);
//        pHeaders.printf("CREATE PROCEDURE %s (%s) AS BEGIN END;\n\n", name, parameters);
        
        pMain.printf("DROP PROCEDURE %s;\n", name);
        pMain.printf("CREATE PROCEDURE %s (%s) AS\n", name, parameters);
    }
    
    private void writeTraceEntry() {
        declareProcedure(
                id.getWithPrefix(ENTRY_SUFFIX),
                procedure.get("Parameters").getValue());
        print("BEGIN\n");
        print("DECLARE %s INT;\n", V_TraceId);
        print("DECLARE %s INT DEFAULT 0;\n", V_Step);
        print("SELECT COALESCE(MAX(id),0)+1 INTO %s FROM %s;\n", V_TraceId, T_Trace);
        print("INSERT INTO %s(id, name) VALUES (:%s, '%s');\n",
                    T_Trace, V_TraceId, id.getValue());
        
        print("CALL %s (", id.getWithPrefix(TRACE_SUFFIX));
        List<String> names = procedure.<Parameters>get("Parameters").getParameterNames();
        print(":%s, 0, %s", V_TraceId, V_Step);
        if (!names.isEmpty()) {
            print(", ");
            print(String.join(",", names));
        }
        print(");\n");
        print("END;\n\n");
    }
    
    private void writeTracedProcedure() {
        Parameters params = procedure.get("Parameters");
        declareProcedure(id.getWithPrefix(TRACE_SUFFIX), 
                String.format("IN %s INT, IN %s INT, INOUT %s INT", V_TraceId, V_Call_Entry_Step, V_Step),
                params.getValue()); // params.count("Parameter") > 0 ?
        print("BEGIN\n");
        enterDeclareBlock();
        traceEntry();
        m_print("DECLARE %s INT := %s;\n", V_Entry_Step, curStep());
        m_print("DECLARE %s TIMESTAMP;\n", V_TimeStamp);
        m_print("DECLARE %s INT;\n", V_Count);
        m_print("\n");
        
        for (Parameter p: params.<Parameter>getAll("Parameter")) {
            if (!p.isOut()) {
                traceArgument(p);
            }
        }
        Node statements = procedure.get("Statements");
        traceStatements(statements.getChildren());
        
        exitDeclareBlock();
        print("END;\n\n");
    }
    
    private void traceStatements(List<Node> statements) {
        for (Node n: statements) {
            declareMode(n.getKey().startsWith("Declare"));
            switch (n.getKey()) {
                case "DeclareCursor":
                    traceDeclareCursor(n);
                    break;
                case "DeclareVariable":
                    traceDeclareVariable(n);
                    break;
                case "AssignmentQuery":
                    traceAssignmentQuery(n);
                    break;
                case "SelectInto":
                    traceSelectInto(n);
                    break;
                case "Assignment":
                    traceAssignment(n);
                    break;
                case "Call":
                    traceCall(n);
                    break;
                case "Block":
                    traceBlock(n);
                    break;
                default:
                    curStep();
                    print(n.getValue());
                    print("\n");
            }
        }
    }
    
    private void traceEntry() {
        incStep();
        traceStep(V_Call_Entry_Step, str("ENTER"), str(id.getValue()), null, procedure.getStartLine());
    }
    
    private void traceArgument(Parameter p) {
        scalars.put(p.getName().getValue(), "?");
        traceVariableValue(p.getName().getValue(), p.getStartLine());
    }
    
    private void traceDeclareCursor(Node n) {        
        incStep();
        String qry = n.getValue().replaceAll("\\s+", " ");
        traceStep(str("CURSOR"), str(n.get("Identifier").getValue()), str(qry), n.getStartLine());
        Node query = n.get("Query");
        
        
        addQueryView(n.get("Identifier").getValue(), n.getStartLine(), n.get("Query").getValue());
        
        traceAfterQuery(query, n.get("Identifier").getValue());
        traceBeforeQuery(query);
        
        m_print(n.getValue());
        m_print("\n");
    }
    
    private void traceDeclareVariable(Node n) {
        scalars.put(n.get("Identifier").getValue(), "?");
        
        m_print(n.getValue());
        m_print("\n");
        
        if (n.has("AssignmentExpression")) {
            incStep();
            traceVariableValue(n.get("Identifier").getValue(), n.getStartLine());
        }
    }
    
    private void traceAssignmentQuery(Node n) {
        incStep();
        String qry = n.getValue().replaceAll("\\s+", " ");
        traceStep(str("QUERY"), str(n.get("Identifier").getValue()), str(qry), n.getStartLine());
        Node query = n.get("SelectQuery");
        
        addQueryView(n.get("Identifier").getValue(), n.getStartLine(), n.get("SelectQuery").getValue());
        
        traceBeforeQuery(query);
        print(n.getValue());
        print("\n");
        
        traceAfterQuery(query, n.get("Identifier").getValue());
    }
    
    private void traceSelectInto(Node n) {
        traceAssignment(n);
    }
    
    private void traceAssignment(Node n) {
        scalars.put(n.get("Identifier").getValue(), "?");
        incStep();
        
        print(n.getValue());
        print("\n");
        traceVariableValue(n.get("Identifier").getValue(), n.getStartLine());
    }
    
    private void traceCall(Node n) {
        incStep();
        Identifier cId = n.get("Identifier");
        traceStep(str("CALL"), str("?"), str(cId.getValue()), n.getStartLine());
        
        print("CALL %s(%s", cId.getWithPrefix(TRACE_SUFFIX), V_Step);
        pHeaders.printf("CREATE PROCEDURE %s(INOUT %s INTEGER",
                    cId.getWithPrefix(TRACE_SUFFIX), V_Step);
        
        Node args = n.get("Arguments");
        if (!args.getValue().trim().isEmpty()) {
            print(", %s", args.getValue());
            pHeaders.printf(", %s", args.getValue());
        }
        print(");\n");
        pHeaders.printf(") AS -- TODO: fix parameters\nBEGIN\n%s := %s+0;\n", V_Step, V_Step);
        pHeaders.printf("CALL %s( );\n", cId.getValue());
        pHeaders.printf("END;\n\n");
    }
    
    private void traceBlock(Node n) {
        print("BEGIN\n");
        Node statements = n.get("Statements");
        traceStatements(statements.getChildren());
        print("END;\n");
    }
    
    private void traceBeforeQuery(Node n) {
        Set<String> names = new HashSet<>();
        for (Node v: n.getAll("VarRef")) {
            String name = v.getValue().replace(":", "");
            if (names.add(name)) {
                print("/*   */ INSERT INTO %s (trace_id,step,id,name,value) "
                            + "VALUES (:%s,:%s,%s,%s,:%s);\n",
                        T_QueryArg,
                        V_TraceId, curStep(), names.size(), 
                        str(name), name);
            }
        }
        print("/*   */ %s := NOW();\n", V_TimeStamp);
    }
    
    private void traceAfterQuery(Node n, String target) {
        print("/*   */ SELECT COUNT(*) INTO %s FROM :%s;\n", V_Count, target);
        print("/*%3d*/ INSERT INTO %s (trace_id,step,query,count,pre,post) "
                    + "VALUES (:%s,:%s,%s,%s,%s,NOW());\n", 
                n.getStartLine(), T_Query,
                V_TraceId, curStep(),
                str(id.getWithPrefix("_" + target + "_" + n.getStartLine() + "_")),
                V_Count, V_TimeStamp);
        
    }
    
    private void traceVariableValue(String name, int line) {
        traceStep(str("VARIABLE"), str(name), ":" + name, line);
    }
    
    private void traceStep(String type, String target, String value, int line) {
        traceStep(V_Entry_Step, type, target, value, line);
    }
    
    private void traceStep(String vEntryStep, String type, String target, String value, int line) {
        print("/*%3d*/ INSERT INTO %s (trace_id,step,entry_step,type,target,value,line) "
                    + "VALUES (:%s,:%s,:%s,%s,%s,%s,%d);\n",
                line, T_Step, 
                V_TraceId, curStep(), vEntryStep, 
                type != null ? type : "NULL",
                target != null ? target : "NULL", 
                value != null ? value : "NULL",
                line
                );
    }
    
    private String curStep() {
        if (incSteps > 0) {
            if (postDeclareMode) {
                return V_Step + "+" + incSteps;
            }
            print("%s := %s + %d;\n", V_Step, V_Step, incSteps);
            incSteps = 0;
        }
        return V_Step;
    }
    
    private String str(String str) {
        return str == null ? null : 
                "'" + str.replace("'", "''")
                         .replace("\n", "\\n") 
                + "'";
    }
    
    private void incStep() {
        incSteps++;
    }
    
    private void addQueryView(String var, int line, String query) {
        queries.computeIfAbsent(var, v -> new HashMap<>())
                .put(line, query);
    }
    
    private void writeVariableViews() {
        print("\n-- VARS --\n\n\n");
        for (String var: scalars.keySet()) {
            String fName = id.getWithFixes("_V__", "_" + var + "_");
            print("DROP FUNCTION %s;\n", fName);
            print("CREATE FUNCTION %s (IN trace_id INT, IN curstep INT)\n", fName);
            print("RETURNS VARCHAR(100) LANGUAGE SQLSCRIPT AS\n");
            print("BEGIN\n");
            print("  SELECT value INTO RESULT FROM %s(:trace_id,:curstep)\n", F_Snapshot);
            print("         WHERE target = '%s';\n", var);
            print("END;\n\n");
        }
        queries.forEach((var, occurrances) -> {
            occurrances.forEach((line, qry) -> {
                String fName = id.getWithFixes("_Q__", "_" + var + "_" + line + "_");
                print("DROP FUNCTION %s;\n", fName);
                print("CREATE FUNCTION %s (IN trace_id INT, IN curstep INT)\n", fName);
                print("RETURNS TABLE(/* --- */) LANGUAGE SQLSCRIPT AS\n");
                print("BEGIN\n");
                print("  RETURN %s;\n", qry);
                print("END;\n\n");
            });
        });
    }
}
