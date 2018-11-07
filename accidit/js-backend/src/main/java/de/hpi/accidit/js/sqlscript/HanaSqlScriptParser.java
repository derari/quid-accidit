package de.hpi.accidit.js.sqlscript;

import de.hpi.accidit.js.parser.Hidden;
import de.hpi.accidit.js.parser.Node;
import de.hpi.accidit.js.parser.Parser;

/**
 *
 */
public class HanaSqlScriptParser extends Parser {

    public HanaSqlScriptParser() {
    }

    @Override
    protected void initialize() {
        super.initialize();
        addRule("WS", "/--[^\\n]*\n/", Hidden.as("WHITESPACE"));
        addRule("WS", "'/*' /([^*]|\\*[^//])*+/ '*/'", Hidden.as("WHITESPACE"));
        
        addRule("QIdentPart", "/\"([^\\\\\"]|\\\\.)+\"/");
        addRule("IdentPart", "/[_\\w]+/", "QIdentPart");
        addRule("Identifier", Identifier::new, 
                    "IdentPart '.' IdentPart", 
                    "IdentPart");
        addRule("QuotedIdentifier", Identifier::new, 
                    "QIdentPart '.' IdentPart",
                    "QIdentPart");
        
        addRule("Type", "Identifier '(' /\\d+/ (',' /\\d+/)? ')'", "Identifier");
        
        addRule("String", "/'([^\\\\']|\\\\.)*'/");
        
        addRule("Parameter", Parameter::new, "('IN'~|'OUT'~|'INOUT'~)? Identifier ~ Type");
        addRule("Parameters", Parameters::new, "Parameter { ',' }");
        
        addRule("Argument", "Identifier | /\\d+/ | String | VarRef");
        addRule("Arguments", "Argument { ',' }");
        
        addRule("VarRef", "':'_/\\w+/ '.' Identifier", "':'_/\\w+/");
        addRule("OtherCode", "/[^;\"':]+/");
        addRule("OtherCodeBeforeInto", "/([^;\"':;I]|I(?!NTO))+/i");
        addRule("OtherCodeBeforeThen", "/([^;\"':;T]|T(?!HEN))+/i");
        
        addRule("GenericExpression", "(String|Identifier|VarRef|OtherCode) { }");
        addRule("SelectBeforeIntoExpression", "(String|VarRef|QuotedIdentifier|OtherCodeBeforeInto) { }");
        addRule("CodeBeforeThenExpression", "(String|VarRef|QuotedIdentifier|OtherCodeBeforeThen) { }");
        
        addRule("AssignmentExpression", "GenericExpression");
        addRule("SqlExpression", "GenericExpression");
        
        addRule("Query", "GenericExpression");
        addRule("SelectQuery", "'SELECT'i ~ Query");
        
        addRule("DbStatement", "('UPDATE'i|'INSERT'i|'DELETE'i) ~ Query { } ';'");
        
        addRule("SelectInto", "'SELECT'i SelectBeforeIntoExpression 'INTO'i Identifier Query ';'");
        
        addRule("DeclareCursor", "'DECLARE'i 'CURSOR'i Identifier 'FOR'i Query ';'");
        addRule("DeclareVariable",
                "'DECLARE'i Identifier 'CONSTANT'i ? Type ('DEFAULT'i|':=') AssignmentExpression ';'", 
                "'DECLARE'i Identifier Type ';'");
        
        addRule("AssignmentQuery", "Identifier (':='|'=') SelectQuery ';'");
        addRule("Assignment", "Identifier (':='|'=') AssignmentExpression ';'");
        
        addRule("Call", "'CALL'i Identifier '(' Arguments ')' ';'");
        
        addRule("ForCursor", "'FOR'i Identifier 'AS'i Identifier 'DO'i");
        addRule("If", "'IF'i CodeBeforeThenExpression 'THEN'i");
        addRule("Else", "'ELSE'i");
        
        addRule("Return", "'RETURN'i GenericExpression ';'");
        addRule("End", "'END'i ('FOR'i|'IF'i) ';'");
        
        addRule("Block", "'BEGIN'i Statements 'END'i ';'");
        
        addAlias("Statement", 
                "DbStatement",
                "SelectInto",
                "DeclareCursor",
                "DeclareVariable",
                "Call",
                "AssignmentQuery",
                "Assignment",
                "ForCursor",
                "If", "Else",
                "End", "Return",
                "Block");
                
        addRule("StmtList", "Statement { }");
        addRule("Statements", "StmtList?");
        
        addRule("StoredProcedure", " 'CREATE'i ? 'PROCEDURE'i ~ Identifier '(' Parameters? ')' "
                + "'LANGUAGE SQLSCRIPT'i ? "
                + "'SQL SECURITY DEFINER'i ? "
                + "'SQL SECURITY INVOKER'i ? "
                + "('DEFAULT SCHEMA'i IdentPart) ? "
                + "'READS SQL DATA'i ? "
                + "'AS'i "
                + "'BEGIN'i Statements 'END'i ';' ");

    }

    public Node parse(String text) {
        return parse(text ,"StoredProcedure");
    }
}
