package de.lundlucenany9;

import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.FunctionParameter;
import de.lundlucenany9.ast.Program;
import de.lundlucenany9.ast.Statement;
import de.lundlucenany9.ast.expressions.*;
import de.lundlucenany9.ast.statements.*;

import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    private String scope = "local";
    private final Set<String> definedNames = new HashSet<>();
    private final Set<String> functionNames = new HashSet<>();
    private final Set<String> functionParameters = new HashSet<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Program parse() {
        List<ImportStatement> imports = new ArrayList<>();
        List<Statement> statements = new ArrayList<>();

        while (match(TokenType.IMPORT)) {
            imports.add(parseImport());
        }

        while (!isAtEnd()) {
            statements.add(parseStatement());
        }

        return new Program(imports, statements);
    }


    private ImportStatement parseImport() {
        String module = consume(TokenType.IDENTIFIER, "Expected module name").lexeme();
        skipNewlines();
        return new ImportStatement(module);
    }

    private Statement parseStatement() {
        skipNewlines();

        if(match(TokenType.GLOBAL)) {
            String previousScope = scope;
            scope = "glob";

            Statement result = null;
            if(match(TokenType.VAR))
                result = parseVar (true, false);
            else if (match(TokenType.ROVAR))
                result = parseVar (true, true);
            //else if (match(TokenType.FUNC))
            //    result = parseGlobalFunction();
            else
                throw error(peek(), "Expected 'var' or 'rovar'");

            scope = previousScope;
            return result;
        } else if(match(TokenType.VAR)) return parseVar(false, false);
        else if(match(TokenType.ROVAR)) return parseVar(false, true);
        else if(match(TokenType.FUNC)) return parseFunction();
        else if(match(TokenType.IF)) return parseIf();
        else if (match(TokenType.FOR)) return parseFor();
        else if (match(TokenType.FOREACH)) return parseForEach();
        else if (match(TokenType.RETURN)) return parseReturn();
        else return parseExpressionStatement();
    }

    private void registerVariable(String name) {
        String fullName = scope + "." + name;
        if(scope.equals("func") && functionParameters.contains(name))
            throw error(peek(), "Parameter '" + name + "' cannot be redefined as a variable");
        if(definedNames.contains(fullName))
            throw error(peek(), scope + " variable '" + name + "' is already defined");
        definedNames.add(fullName);
    }

    private void unregisterVariable(String name) {
        String fullName = scope + "." + name;
        definedNames.remove(fullName);
    }

    private void unregisterFuncVariables() {
        definedNames.removeIf(name -> name.startsWith("func" + "."));
    }

    private void registerFunction(String name) {
        String fullName = scope + "." + name;
        if(functionNames.contains(fullName))
            throw error(peek(), scope + " function '" + name + "' is already defined");
        functionNames.add(fullName);
    }

    private VarDeclaration parseVar(boolean isGlobal, boolean isReadOnly) {
        String name = expect(TokenType.IDENTIFIER).lexeme();
        expect(TokenType.EQUAL);
        registerVariable(name);

        ExpressionNode initializer = parseExpression();
        skipNewlines();
        return new VarDeclaration((isGlobal ? "glob" : scope) + "." + name, initializer, isReadOnly);
    }

    private FunctionDeclaration parseGlobalFunction() {
        if(scope.equals("func")) throw error(peek(), "Functions cannot be nested");
        String name = expect(TokenType.IDENTIFIER).lexeme();
        name = scope + "." + name;
        registerFunction(name);

        FunctionDeclaration func = parseFuncBody(name);
        return new FunctionDeclaration(name, func.parameters(), func.optionalParams(), func.body(), true);
    }

    private FunctionDeclaration parseFunction() {
        if(scope.equals("func")) throw error(peek(), "Functions cannot be nested");
        String name = expect(TokenType.IDENTIFIER).lexeme();
        name = scope + "." + name;
        registerFunction(name);

        return parseFuncBody(name);
    }

    private FunctionDeclaration parseFuncBody(String name) {
        expect(TokenType.LEFT_PAREN);

        List<FunctionParameter> parameters = new ArrayList<>();
        Map<String, ExpressionNode> optionalParams = new LinkedHashMap<>();

        functionParameters.clear();

        if(!check(TokenType.RIGHT_PAREN)) {
            do {
                boolean isReference = match(TokenType.HASH);
                String paramName = expect(TokenType.IDENTIFIER).lexeme();
                parameters.add(new FunctionParameter(paramName, isReference));
                functionParameters.add(paramName);
            }while (match(TokenType.COMMA));
        }

        expect(TokenType.RIGHT_PAREN);

        if(match(TokenType.LEFT_BRACE)) {
            if(!check(TokenType.RIGHT_BRACE)) {
                do {
                    String key = expect(TokenType.IDENTIFIER).lexeme();
                    expect(TokenType.EQUAL);
                    ExpressionNode value = parseExpression();
                    optionalParams.put(key, value);
                } while (match(TokenType.COMMA));
            }
            expect(TokenType.RIGHT_BRACE);
        }

        skipNewlines();

        expect(TokenType.INDENT);

        String previousScope = scope;
        scope = "func";

        List<Statement> body = new ArrayList<>();
        skipNewlines();
        while (!check(TokenType.DEDENT) && !isAtEnd()) {
            body.add(parseStatement());
        }

        unregisterFuncVariables();

        scope = previousScope;
        functionParameters.clear();

        expect(TokenType.DEDENT);
        skipNewlines();
        return new FunctionDeclaration(name, parameters, optionalParams, body, false);
    }

    private IfStatement parseIf() {
        ExpressionNode condition = parseExpression();
        skipNewlines();
        expect(TokenType.INDENT);
        List<Statement> thenBranch = new ArrayList<>();
        while (!check(TokenType.DEDENT) && !isAtEnd()) {
            thenBranch.add(parseStatement());
        }
        skipNewlines();
        expect(TokenType.DEDENT);
        skipNewlines();
        List<IfElseClause> elifClauses = new ArrayList<>();
        while (match(TokenType.ELIF)) {
            ExpressionNode elifCondition = parseExpression();
            skipNewlines();
            expect(TokenType.INDENT);
            List<Statement> elifBranch = new ArrayList<>();
            while (!check(TokenType.DEDENT) && !isAtEnd()) {
                elifBranch.add(parseStatement());
            }
            expect(TokenType.DEDENT);
            skipNewlines();
            elifClauses.add(new IfElseClause(elifCondition, elifBranch));
        }
        List<Statement> elseBranch = new ArrayList<>();
        if(match(TokenType.ELSE)) {
            skipNewlines();
            expect(TokenType.INDENT);
            while (!check(TokenType.DEDENT) && !isAtEnd()) {
                elseBranch.add(parseStatement());
            }
            expect(TokenType.DEDENT);
            skipNewlines();
        }
        return new IfStatement(condition, thenBranch, elifClauses, elseBranch);
    }
    private ForLoop parseFor() {
        match(TokenType.ROVAR);
        String variable = expect(TokenType.IDENTIFIER).lexeme();
        registerVariable(variable);

        expect(TokenType.ARROW);
        RangeLiteral range = parseRange();
        skipNewlines();
        expect(TokenType.INDENT);

        List<Statement> body = new ArrayList<>();
        while (!check(TokenType.DEDENT) && !isAtEnd()) {
            body.add(parseStatement());
        }
        expect(TokenType.DEDENT);
        unregisterVariable(variable);
        skipNewlines();

        return new ForLoop(variable, range, body);
    }

    private ForEachLoop parseForEach() {
        expect(TokenType.ROVAR);
        String valueVar = expect(TokenType.IDENTIFIER).lexeme();
        registerVariable(valueVar);


        expect(TokenType.COMMA);
        expect(TokenType.ROVAR);
        String indexVar = expect(TokenType.IDENTIFIER).lexeme();
        registerVariable(indexVar);


        expect(TokenType.ARROW);

        ExpressionNode iterable = parseExpression();
        skipNewlines();
        expect(TokenType.INDENT);

        List<Statement> body = new ArrayList<>();
        while (!check(TokenType.DEDENT) && !isAtEnd()) {
            body.add(parseStatement());
        }
        expect(TokenType.DEDENT);
        unregisterVariable(valueVar);
        if(indexVar != null) unregisterVariable(indexVar);
        skipNewlines();

        return new ForEachLoop(valueVar, indexVar, iterable, body);
    }

    private ReturnStatement parseReturn() {
        ExpressionNode value = null;
        if(!check(TokenType.NEWLINE) && !check(TokenType.EOF) && !check(TokenType.DEDENT) && canStartExpression()) {
            value = parseExpression();
        }
        skipNewlines();
        return new ReturnStatement(value);
    }

    private Statement parseExpressionStatement() {
        ExpressionNode expr = parseExpression();
        skipNewlines();
        return new ExpressionStatement(expr);
    }

    private ExpressionNode parseExpression() {
        return parseAssignment();
    }

    private ExpressionNode parseAssignment() {
        ExpressionNode expr = parseOr();
        if(match(TokenType.EQUAL)) {
            ExpressionNode value = parseExpression();
            if(expr instanceof VariableAccess var) {
                skipNewlines();
                return new Assignment(var.name(), value);
            } else if(expr instanceof IndexAccess index) {
                skipNewlines();
                return new IndexedAssignment(index.target(), index.index(), value);
            } else {
                throw error(previous(), "Invalid assignment target");
            }
        }
        return expr;
    }

    private ExpressionNode parseOr() {
        ExpressionNode expr = parseAnd();

        while (match(TokenType.OR)) {
            TokenType operator = previous().type();
            ExpressionNode right = parseAnd();
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ExpressionNode parseAnd() {
        ExpressionNode expr = parseEquality();

        while (match(TokenType.AND)) {
            TokenType operator = previous().type();
            ExpressionNode right = parseEquality();
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ExpressionNode parseEquality() {
        ExpressionNode expr = parseComparison();

        while (match(TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL)) {
            TokenType operator = previous().type();
            ExpressionNode right = parseComparison();
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ExpressionNode parseComparison() {
        ExpressionNode expr = parseAdditive();

        while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            TokenType operator = previous().type();
            ExpressionNode right = parseAdditive();
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ExpressionNode parseAdditive() {
        ExpressionNode expr = parseMultiplicative();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            TokenType operator = previous().type();
            ExpressionNode right = parseMultiplicative();
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ExpressionNode parseMultiplicative() {
        ExpressionNode expr = parseUnary();

        while (match(TokenType.STAR, TokenType.SLASH)) {
            TokenType operator = previous().type();
            ExpressionNode right = parseUnary();
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ExpressionNode parseUnary() {
        if(match(TokenType.NOT, TokenType.MINUS)) {
            TokenType operator = previous().type();
            ExpressionNode right = parseUnary();
            return new UnaryOp(operator, right);
        }
        return parsePostfix();
    }

    private ExpressionNode parsePostfix() {
        ExpressionNode expr = parsePrimary();

        while (true) {
            if(match(TokenType.LEFT_BRACKET)) {
                ExpressionNode index = parseExpression();
                if(!(index instanceof Literal) && !(index instanceof VariableAccess)) {
                    throw error(previous(), "Index must be a literal or variable access");
                }
                expect(TokenType.RIGHT_BRACKET);
                expr = new IndexAccess(expr, index);
            } /*else if(match(TokenType.LEFT_BRACE)) {
                String property = expect(TokenType.IDENTIFIER).lexeme();
                expect(TokenType.RIGHT_BRACE);
                expr = new IndexAccess(expr, new Literal(property));
            } */else {
                break;
            }
        }
        return expr;
    }

    private ExpressionNode parsePrimary() {
        if(match(TokenType.NULL)) return new Literal(null);
        if(match(TokenType.INTEGER)) return new Literal(Integer.parseInt(previous().lexeme()));
        if(match(TokenType.DECIMAL)) return new Literal(Double.parseDouble(previous().lexeme()));
        if(match(TokenType.STRING)) return new Literal(previous().lexeme().substring(1, previous().lexeme().length() - 1).replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r"));
        if(match(TokenType.INTERPOLATED_STRING)) return parseInterpolatedString(previous().lexeme());
        if(match(TokenType.LEFT_BRACKET)) return parseArrayLiteral();
        if(match(TokenType.LEFT_BRACE)) return parseMapLiteral();
        if(match(TokenType.TRUE)) return new Literal(true);
        if(match(TokenType.FALSE)) return new Literal(false);
        if(match(TokenType.DOLLAR)) {
            StringBuilder name = new StringBuilder(expect(TokenType.IDENTIFIER).lexeme());
            while (match(TokenType.DOT)) {
                name.append(".").append(expect(TokenType.IDENTIFIER).lexeme());
            }
            return new VariableAccess(name.toString());
        }
        if(match(TokenType.HASH)) {
            StringBuilder name = new StringBuilder(expect(TokenType.IDENTIFIER).lexeme());
            while (match(TokenType.DOT)) {
                name.append(".").append(expect(TokenType.IDENTIFIER).lexeme());
            }
            return new VariableReference(name.toString());
        }
        if(match(TokenType.LEFT_PAREN)) {
            ExpressionNode expr = parseExpression();
            expect(TokenType.RIGHT_PAREN);
            return expr;
        }
        if(check(TokenType.IDENTIFIER)) return parseFunctionCallOrVariable();

        throw error(peek(), "Unexpected token: " + peek().type().getLexeme());
    }

    private ExpressionNode parseFunctionCallOrVariable() {
        StringBuilder name = new StringBuilder(expect(TokenType.IDENTIFIER).lexeme());

        while (match(TokenType.DOT)) {
            String next = expect(TokenType.IDENTIFIER).lexeme();
            name.append(".").append(next);
        }

        List<ExpressionNode> arguments = new ArrayList<>();
        Map<String, ExpressionNode> optionalArguments = new HashMap<>();

        if(!check(TokenType.LEFT_BRACE) && !check(TokenType.NEWLINE) && !check(TokenType.DEDENT) && !check(TokenType.EOF) && !check(TokenType.RIGHT_PAREN) && !check(TokenType.RIGHT_BRACKET) && !check(TokenType.COMMA) && canStartExpression()) {
            do {
                arguments.add(parseOr());
            } while (canContinueArguments());
        }

        if(match(TokenType.LEFT_BRACE)) {
            if(!check(TokenType.RIGHT_BRACE)) {
                do {
                    String key = expect(TokenType.IDENTIFIER).lexeme();
                    expect(TokenType.EQUAL);
                    ExpressionNode value = parseExpression();
                    optionalArguments.put(key, value);
                } while (match(TokenType.COMMA));
            }
            expect(TokenType.RIGHT_BRACE);
        }

        return new FunctionCall(name.toString(), arguments, optionalArguments);
    }

    private ArrayLiteral parseArrayLiteral() {
        List<ExpressionNode> elements = new ArrayList<>();

        if(!check(TokenType.RIGHT_BRACKET)) {
            do {
                elements.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RIGHT_BRACKET);

        return new ArrayLiteral(elements);
    }

    private MapLiteral parseMapLiteral() {
        Map<String, ExpressionNode> entries = new HashMap<>();

        if(!check(TokenType.RIGHT_BRACE)) {
            do {
                String key = expect(TokenType.STRING).lexeme();
                key = key.substring(1, key.length() - 1);
                expect(TokenType.EQUAL);
                ExpressionNode value = parseExpression();
                entries.put(key, value);
            } while (match(TokenType.COMMA));
        }
        expect(TokenType.RIGHT_BRACE);

        return new MapLiteral(entries);
    }

    private RangeLiteral parseRange() {
        int start = Integer.parseInt(expect(TokenType.INTEGER).lexeme());
        expect(TokenType.MINUS);
        int end = Integer.parseInt(expect(TokenType.INTEGER).lexeme());
        return new RangeLiteral(start, end);
    }

    private InterpolatedString parseInterpolatedString(String literal) {
        literal = literal.substring(2, literal.length() - 1);

        List<StringPart> parts = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        int i = 0;

        while(i < literal.length()) {
            if(i < literal.length() - 1 && literal.charAt(i) == '$') {
                if(!currentText.isEmpty()) {
                    parts.add(new StringPart(false, currentText.toString().replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r"), null));
                    currentText = new StringBuilder();
                }

                i++;

                if(literal.charAt(i) == '(') {
                    i++;
                    StringBuilder exprStr = new StringBuilder();
                    int depth = 1;
                    while(i < literal.length() && depth > 0) {
                        char ch = literal.charAt(i);
                        if(ch == '(') depth++;
                        else if(ch == ')') depth--;

                        if(depth > 0) {
                            exprStr.append(ch);
                        }
                        i++;
                    }

                    Parser parser = new Parser(new Lexer(exprStr.toString()).tokenize());
                    ExpressionNode expr = parser.parseExpression();
                    parts.add(new StringPart(true, null, expr));
                } else if (Character.isLetter(literal.charAt(i))) {
                    StringBuilder varName = new StringBuilder();
                    while (i < literal.length() && (Character.isLetterOrDigit(literal.charAt(i)) || literal.charAt(i) == '_' || literal.charAt(i) == '.')) {
                        varName.append(literal.charAt(i));
                        i++;
                    }
                    parts.add(new StringPart(true, null, new VariableAccess(varName.toString())));
                } else {
                    currentText.append('$');
                }
            } else {
                currentText.append(literal.charAt(i));
                i++;
            }
        }
        if(!currentText.isEmpty()) {
            parts.add(new StringPart(false, currentText.toString().replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r"), null));
        }

        return new InterpolatedString(parts);
    }

    private boolean match(TokenType... type) {
        for (TokenType t : type) {
            if(check(t)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if(isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if(!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if(check(type)) return advance();
        throw error(peek(), message);
    }

    private Token expect(TokenType type) {
        if(check(type)) return advance();
        throw error(peek(), "Expected " + type.getLexeme() + " but got " + peek().type().getLexeme() + ": '" + peek().lexeme() + "'");
    }

    private void skipNewlines() {
        while (match(TokenType.NEWLINE)) ;
    }

    private EESSyntaxException error(Token token, String message) {
        return new EESSyntaxException(message + " at " + token.line() + ":" + token.column());
    }

    private boolean canStartExpression() {
        return check(TokenType.DOLLAR) || check(TokenType.HASH) ||
                check(TokenType.INTEGER) || check(TokenType.DECIMAL) ||
                check(TokenType.STRING) || check(TokenType.INTERPOLATED_STRING) ||
                check(TokenType.LEFT_BRACKET) || check(TokenType.LEFT_BRACE) ||
                check(TokenType.LEFT_PAREN) || check(TokenType.IDENTIFIER) ||
                check(TokenType.NOT) || check(TokenType.MINUS) || check(TokenType.NULL);
    }

    private boolean canContinueArguments() {
        return (check(TokenType.DOLLAR) || check(TokenType.HASH) ||
                check(TokenType.INTEGER) || check(TokenType.DECIMAL) ||
                check(TokenType.STRING) || check(TokenType.LEFT_BRACKET) ||
                check(TokenType.LEFT_PAREN) || check(TokenType.IDENTIFIER) ||
                check(TokenType.NOT) || check(TokenType.MINUS)) &&
                !check(TokenType.LEFT_BRACE);
    }

}
