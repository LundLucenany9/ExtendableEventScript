
public record Token(TokenType type, String lexeme, int line, int column) {
    @Override
    public String toString() {
        return type.name();
        //return type.name() + ": " + line  + ":" + column + " : '" + lexeme + "'";
    }
}