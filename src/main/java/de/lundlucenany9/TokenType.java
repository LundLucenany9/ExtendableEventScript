package de.lundlucenany9;

public enum TokenType {

    // Keywords
    IMPORT("'import'"),
    GLOBAL("'global'"),
    VAR("'var'"),
    ROVAR("'rovar'"),
    FUNC("'func'"),
    IF("'if'"),
    ELIF( "'elif'"),
    ELSE("'else'"),
    FOREACH( "'foreach'"),
    FOR("'for'"),
    RETURN("'return'"),

    // Literals
    IDENTIFIER("identifier"),
    DECIMAL( "decimal"),
    INTEGER( "integer"),
    STRING( "string"),
    INTERPOLATED_STRING( "interpolated string"),
    NULL( "'null'"),
    TRUE( "'true'"),
    FALSE( "'false'"),

    OR( "'or'"),
    AND( "'and'"),

    // Operators
    EQUAL("'='"),
    PLUS("'+'"),
    MINUS("'-'"),
    STAR("'*'"),
    SLASH("'/''"),
    NOT("'!'"),
    LESS("'<'"),
    GREATER("'>'"),
    EQUAL_EQUAL("'=='"),
    NOT_EQUAL("'!='"),
    LESS_EQUAL("'<='"),
    GREATER_EQUAL("'>='"),

    // Special
    DOLLAR("'$'"),
    HASH("'#'"),
    DOT("'.'"),
    ARROW("'->'"),

    // Delimiters
    LEFT_PAREN("'('"),
    RIGHT_PAREN("')'"),
    LEFT_BRACKET("'['"),
    RIGHT_BRACKET("']'"),
    LEFT_BRACE("'{'"),
    RIGHT_BRACE("'}'"),
    COMMA("','"),

    INDENT("indent"),
    DEDENT("dedent"),
    NEWLINE("newline"),

    EOF("End of File");

    private final String lexeme;

    TokenType(String lexeme) {
        this.lexeme = lexeme;
    }

    public String getLexeme() {
        return lexeme;
    }
}
