public enum TokenType {

    // Keywords
    IMPORT,
    GLOBAL,
    VAR,
    FUNC,
    IF,
    ELIF,
    ELSE,
    FOREACH,
    FOR,
    RETURN,

    // Literals
    IDENTIFIER,
    DECIMAL,
    INTEGER,
    STRING,
    INTERPOLATED_STRING,
    NULL,

    // Operators
    EQUAL,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    NOT,
    LESS,
    GREATER,
    EQUAL_EQUAL,
    NOT_EQUAL,
    LESS_EQUAL,
    GREATER_EQUAL,

    // Special
    DOLLAR,
    HASH,
    DOT,
    ARROW,

    // Delimiters
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    LEFT_BRACE,
    RIGHT_BRACE,
    COMMA,

    INDENT,
    DEDENT,
    NEWLINE,

    EOF
}
