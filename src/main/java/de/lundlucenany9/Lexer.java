package de.lundlucenany9;

import java.util.*;

public class Lexer {

    private static final Map<String, TokenType> KEYWORDS;
    static {
        KEYWORDS = new HashMap<>();
        KEYWORDS.put("import", TokenType.IMPORT);
        KEYWORDS.put("global", TokenType.GLOBAL);
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("rovar", TokenType.ROVAR);
        KEYWORDS.put("function", TokenType.FUNC);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("elif", TokenType.ELIF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("foreach", TokenType.FOREACH);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("null", TokenType.NULL);
    }
    private final String source;
    private int start;
    private int current;

    private int line = 1;
    private int column = 1;

    private int tokenColumn;
    private int tokenLine;


    private final Deque<Integer> indents = new ArrayDeque<>();

    private final List<Token> tokens = new ArrayList<>();

    public Lexer(String source) {
        this.source = source.replace("\r\n", "\n");
    }

    public List<Token> tokenize() {
        tokens.clear();
        indents.clear();
        indents.push(0);
        start = 0;
        current = 0;
        line = 1;
        column = 1;

        while (current < source.length()) {
            scanToken();
        }
        while (indents.size() > 1) {
            indents.pop();
            addDedent();
        }

        start = current;
        tokenLine = line;
        tokenColumn = column;
        addToken(TokenType.EOF);
        return tokens;
    }

    private void scanToken() {
        start = current;
        tokenColumn = column;
        tokenLine = line;
        char c = advance();

        switch (c) {
            case '(' -> addToken(TokenType.LEFT_PAREN);
            case ')' -> addToken(TokenType.RIGHT_PAREN);

            case '{' -> addToken(TokenType.LEFT_BRACE);
            case '}' -> addToken(TokenType.RIGHT_BRACE);

            case '[' -> addToken(TokenType.LEFT_BRACKET);
            case ']' -> addToken(TokenType.RIGHT_BRACKET);

            case ',' -> addToken(TokenType.COMMA);
            case '.' -> addToken(TokenType.DOT);

            case '+' -> addToken(TokenType.PLUS);
            case '-' -> addToken(match('>') ? TokenType.ARROW : TokenType.MINUS);
            case '*' -> addToken(TokenType.STAR);
            case '/' -> {
                if(match('/')) {
                    while (!isAtEnd() && peek() != '\n') {
                        advance();
                    }
                    break;
                }
                addToken(TokenType.SLASH);
            }

            case '=' -> addToken(
                    match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL
            );

            case '!' -> addToken(
                    match('=') ? TokenType.NOT_EQUAL : TokenType.NOT
            );

            case '<' -> addToken(
                    match('=') ? TokenType.LESS_EQUAL : TokenType.LESS
            );

            case '>' -> addToken(
                    match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER
            );

            case ' ', '\r' -> {}

            case '\n' -> {
                addToken(TokenType.NEWLINE);

                int indentation = 0;

                while (peek() == ' ') {
                    advance();
                    indentation++;
                }

                // Leerzeile: diese Zeile darf den Indent-Level nicht verändern.
                if (peek() == '\n' || isAtEnd()) {
                    break;
                }

                int previous = indents.peek();

                if (indentation > previous) {
                    indents.push(indentation);
                    addIndent();
                } else if (indentation < previous) {
                    while (indentation < indents.peek()) {
                        indents.pop();
                        addDedent();
                    }

                    if (indentation != indents.peek()) {
                        throw new EESSIndentationError(
                                "Invalid indentation on line " + line
                        );
                    }
                }
            }

            case '"' -> {
                string();
                addToken(TokenType.STRING);
            }

            case '$' ->{
                if(match('"')) {
                    string();
                    addToken(TokenType.INTERPOLATED_STRING);
                } else
                    addToken(TokenType.DOLLAR);
            }

            case '#' -> addToken(TokenType.HASH);

            default -> {
                if (Character.isDigit(c)) {
                    number();
                } else if (isIdentifierStart(c)) {
                    identifier();
                } else {
                    throw new EESSyntaxException("Unexpected character: " + c);
                }
            }
        }
    }

    private void addIndent() {
        tokens.add(new Token(
                TokenType.INDENT,
                "",
                tokenLine,
                tokenColumn
        ));
    }

    private void addDedent() {
        tokens.add(new Token(
                TokenType.DEDENT,
                "",
                tokenLine,
                tokenColumn
        ));
    }
    private void identifier() {
        while (isIdentifierPart(peek())) {
            advance();
        }
        String identifier = source.substring(start, current);

        TokenType type = KEYWORDS.get(identifier);

        if(type == null)
            type = TokenType.IDENTIFIER;

        addToken(type);
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private void number() {
        while (Character.isDigit(peek())) {
            advance();
        }

        if (peek() == '.' && Character.isDigit(peekNext())) {

            do {
                advance();
            } while (Character.isDigit(peek()));

            addToken(TokenType.DECIMAL);
        } else {
            addToken(TokenType.INTEGER);
        }
    }

    private boolean isIdentifierStart(char c) {
        return (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || c == '_';
    }

    private void string() {
        while (!isAtEnd() && peek() != '"') {
            advance();
        }

        if (isAtEnd()) {
            throw new EESSyntaxException(
                    "Unterminated string at " + line + ":" + column
            );
        }

        advance();
    }

    private void addToken(TokenType type) {
        tokens.add(new Token(
                type,
                source.substring(start, current),
                tokenLine,
                tokenColumn
        ));
    }

    private char advance() {
        char c = source.charAt(current++);

        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }

        return c;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }

        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }

        return source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) {
            return false;
        }

        advance();
        return true;
    }

}
