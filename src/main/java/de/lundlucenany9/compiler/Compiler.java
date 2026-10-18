package de.lundlucenany9.compiler;

import de.lundlucenany9.TokenType;
import de.lundlucenany9.ast.ExpressionNode;
import de.lundlucenany9.ast.FunctionParameter;
import de.lundlucenany9.ast.Program;
import de.lundlucenany9.ast.Statement;
import de.lundlucenany9.ast.expressions.*;
import de.lundlucenany9.ast.statements.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Compiler {
    private final BytecodeWriter writer;
    List<String> functionNames;

    public Compiler() {
        writer = new BytecodeWriter();
        functionNames = new ArrayList<>();
    }

    public byte[] compile(Program program) {
        writer.writeBytes("EES\0");
        writer.writeByte((byte) 0x01);
        compileImports(program.getImports());
        compileStatements(program.getStatements());
        return writer.toByteArray();
    }

    private void compileImports(List<ImportStatement> statements) {
        for(ImportStatement statement : statements) {
            writer.writeOpcode(Opcode.IMPORT);
            writer.writeString(statement.module());
        }
    }

    private void compileFunctionDeclaration(FunctionDeclaration func) {
        // Defaults der optionalen Parameter auswerten (in Map-Reihenfolge)
        //for (ExpressionNode def : func.optionalParams().values()) {
        //    compileExpression(def);
        //}

        writer.writeOpcode(Opcode.DEFINE_FUNC);
        writer.writeString(func.name());

        writer.writeByte((byte) func.parameters().size());
        for (FunctionParameter p : func.parameters()) {
            writer.writeBit(p.isReference());
            writer.writeString(p.name());
        }

        writer.writeByte((byte) func.optionalParams().size());
        for (String key : func.optionalParams().keySet()) {
            writer.writeString(key);
            compileExpression(func.optionalParams().get(key));
        }

        int endJump = writer.getOffset();
        writer.writeInt(0);                       // Placeholder: Adresse hinter dem Body

        compileStatements(func.body());           // Body beginnt direkt hier
        if (!endsWithReturn(func.body())) {
            emitImplicitReturn();
        }

        writer.setIntAt(endJump, writer.getOffset());
    }

    private boolean endsWithReturn(List<Statement> body) {
        return !body.isEmpty()
                && body.get(body.size() - 1) instanceof ReturnStatement;
    }

    private void emitImplicitReturn() {
        writer.writeOpcode(Opcode.PUSH);
        writer.writeByte((byte) PushType.NULL.ordinal());
        writer.writeOpcode(Opcode.RET);
    }

    private void compileStatements(List<Statement> statements) {
        for(Statement stmt : statements) {
            compileStatement(stmt);
        }
    }

    private void compileStatement(Statement stmt) {
        if(stmt instanceof VarDeclaration st) compileVarDeclaration(st);
        else if(stmt instanceof IfStatement st) compileIfStatement(st);
        else if(stmt instanceof ForLoop st) compileForLoop(st);
        else if(stmt instanceof ForEachLoop st) compileForeachStatement(st);
        else if(stmt instanceof ReturnStatement st) compileReturnStatement(st);
        else if(stmt instanceof ExpressionStatement st) compileExpressionStatement(st);
        else if(stmt instanceof FunctionDeclaration st) compileFunctionDeclaration(st);
        else throw new CompileException("Unknown statement type: " + stmt.getClass().getName());
    }

    private void compileVarDeclaration(VarDeclaration var) {
        compileExpression(var.initializer());
        writer.writeOpcode(Opcode.DEFINE_VAR);
        writer.writeBit(var.isReadOnly());
        writer.writeString(var.name());
    }

    private void compileIfStatement(IfStatement ifStatement) {
        List<Integer> endJumps = new ArrayList<>();
        boolean hasMoreBranches = !ifStatement.elifClauses().isEmpty()
                || !ifStatement.elseBody().isEmpty();

        // if
        compileExpression(ifStatement.condition());
        writer.writeOpcode(Opcode.JMP_IF_NOT);
        int pendingFalseJump = writer.getOffset();
        writer.writeInt(0);

        compileStatements(ifStatement.thenBody());

        if (hasMoreBranches) {
            writer.writeOpcode(Opcode.JMP);
            endJumps.add(writer.getOffset());
            writer.writeInt(0);
        }
        writer.setIntAt(pendingFalseJump, writer.getOffset());   // Beginn des nächsten Zweigs

        // elif
        List<IfElseClause> elifs = ifStatement.elifClauses();
        for (int i = 0; i < elifs.size(); i++) {
            IfElseClause clause = elifs.get(i);
            boolean last = i == elifs.size() - 1;
            boolean moreAfter = !last || !ifStatement.elseBody().isEmpty();

            compileExpression(clause.condition());
            writer.writeOpcode(Opcode.JMP_IF_NOT);
            pendingFalseJump = writer.getOffset();
            writer.writeInt(0);

            compileStatements(clause.body());

            if (moreAfter) {
                writer.writeOpcode(Opcode.JMP);
                endJumps.add(writer.getOffset());
                writer.writeInt(0);
            }
            writer.setIntAt(pendingFalseJump, writer.getOffset());
        }

        // else
        compileStatements(ifStatement.elseBody());               // leer = nichts

        int end = writer.getOffset();
        for (int p : endJumps) writer.setIntAt(p, end);
    }

    private void compileForLoop(ForLoop forLoop) {
        RangeLiteral range = forLoop.range();
        String var = forLoop.variable();
        boolean ascending = range.end() >= range.start();

        // i = start
        writer.writeOpcode(Opcode.DEFINE_TMP);
        writer.writeString(var);
        pushInt(range.start());
        writer.writeOpcode(Opcode.STORE_TMP);
        writer.writeString(var);

        int loopStart = writer.getOffset();

        // Bedingung: i < end, sonst zum Ende springen
        writer.writeOpcode(Opcode.LOAD);
        writer.writeString(var);
        pushInt(range.end());
        writer.writeOpcode(ascending ? Opcode.LTE : Opcode.GTE);
        writer.writeOpcode(Opcode.JMP_IF_NOT);
        int exitJump = writer.getOffset();
        writer.writeInt(0);  // Placeholder

        compileStatements(forLoop.body());

        // i = i + 1
        writer.writeOpcode(Opcode.LOAD);
        writer.writeString(var);
        pushInt(1);
        writer.writeOpcode(ascending ? Opcode.ADD : Opcode.SUB);
        writer.writeOpcode(Opcode.STORE_TMP);
        writer.writeString(var);

        // Zurück zur Prüfung
        writer.writeOpcode(Opcode.JMP);
        writer.writeInt(loopStart);

        // Exit-Adresse patchen
        writer.setIntAt(exitJump, writer.getOffset());
        writer.writeOpcode(Opcode.UNDEFINE_TMP);
        writer.writeString(var);
    }

    private void compileForeachStatement(ForEachLoop foreach) {
        String indexVar = foreach.indexVar();
        String valueVar = foreach.itemVar();
        ExpressionNode iterable = foreach.iterable();

        writer.writeOpcode(Opcode.DEFINE_TMP);
        writer.writeString(valueVar);
        writer.writeOpcode(Opcode.DEFINE_TMP);
        writer.writeString(indexVar);

        compileExpression(iterable);

        int loopStart = writer.getOffset();

        writer.writeOpcode(Opcode.LOAD_NEXT_INDEX);
        writer.writeString(indexVar);
        writer.writeOpcode(Opcode.PUSH);
        writer.writeByte((byte) PushType.NULL.ordinal());
        writer.writeOpcode(Opcode.EQ);
        writer.writeOpcode(Opcode.JMP_IF);
        int exitJump = writer.getOffset();
        writer.writeInt(0);
        writer.writeOpcode(Opcode.DUP);
        writer.writeOpcode(Opcode.LOAD);
        writer.writeString(indexVar);
        writer.writeOpcode(Opcode.INDEX);
        writer.writeOpcode(Opcode.STORE);
        writer.writeString(valueVar);

        compileStatements(foreach.body());

        writer.writeOpcode(Opcode.JMP);
        writer.writeInt(loopStart);
        writer.setIntAt(exitJump, writer.getOffset());
        writer.writeOpcode(Opcode.UNDEFINE_TMP);
        writer.writeString(valueVar);
        writer.writeOpcode(Opcode.UNDEFINE_TMP);
        writer.writeString(indexVar);
        writer.writeOpcode(Opcode.POP);
    }

    private void compileReturnStatement(ReturnStatement returnStatement) {
        if(returnStatement.value() != null) {
            compileExpression(returnStatement.value());
        } else {
            writer.writeOpcode(Opcode.PUSH);
            writer.writeByte((byte) PushType.UNDEFINED.ordinal());
        }
        writer.writeOpcode(Opcode.RET);
    }

    private void compileExpressionStatement(ExpressionStatement expressionStatement) {
        compileExpression(expressionStatement.expression());
        if(!(expressionStatement.expression() instanceof Assignment || expressionStatement.expression() instanceof IndexedAssignment)) {
            writer.writeOpcode(Opcode.POP);
        }
    }

    private void compileExpression(ExpressionNode expression) {
        if(expression instanceof Assignment assignment) compileAssignment(assignment);
        else if(expression instanceof IndexedAssignment assignment) compileIndexedAssignment(assignment);
        else if(expression instanceof Literal literal) compileLiteral(literal);
        else if(expression instanceof VariableAccess access) compileVariableAccess(access);
        else if(expression instanceof VariableReference reference) compileVariableReference(reference);
        else if(expression instanceof BinaryOp binaryOp) compileBinaryOp(binaryOp);
        else if(expression instanceof UnaryOp unaryOp) compileUnaryOp(unaryOp);
        else if(expression instanceof FunctionCall call) compileFunctionCall(call);
        else if(expression instanceof IndexAccess access) compileIndexAccess(access);
        else if(expression instanceof ArrayLiteral literal) compileArrayLiteral(literal);
        else if(expression instanceof MapLiteral literal) compileMapLiteral(literal);
        else if(expression instanceof InterpolatedString string) compileInterpolatedString(string);
        else throw new CompileException("Unknown expression type: " + expression.getClass().getName());
    }

    private void compileAssignment(Assignment assignment) {
        compileExpression(assignment.value());
        writer.writeOpcode(Opcode.STORE);
        writer.writeString(assignment.variableName());
    }

    private void compileIndexedAssignment(IndexedAssignment assignment) {
        compileExpression(assignment.value());
        compileExpression(assignment.index());
        compileExpression(assignment.target());
        writer.writeOpcode(Opcode.INDEX_ASSIGN);
    }

    private void compileLiteral(Literal literal) {
        writer.writeOpcode(Opcode.PUSH);

        if(literal.value() == null) writer.writeByte((byte) PushType.NULL.ordinal());
        else if(literal.value() instanceof Integer intValue) {
            writer.writeByte((byte) PushType.INTEGER.ordinal());
            writer.writeInt(intValue);
        }
        else if(literal.value() instanceof Double doubleValue) {
            writer.writeByte((byte) PushType.FLOAT.ordinal());
            writer.writeDouble(doubleValue);
        }
        else if(literal.value() instanceof String stringValue) {
            writer.writeByte((byte) PushType.STRING.ordinal());
            writer.writeString(stringValue);
        }
        else if(literal.value() instanceof Boolean boolValue) {
            writer.writeByte((byte) PushType.BOOLEAN.ordinal());
            writer.writeBit(boolValue);
        }
    }

    private void compileVariableAccess(VariableAccess access) {
        writer.writeOpcode(Opcode.LOAD);
        writer.writeString(access.name());
    }

    private void compileVariableReference(VariableReference reference) {
        writer.writeOpcode(Opcode.LOAD_REF);
        writer.writeString(reference.name());
    }

    private void compileBinaryOp(BinaryOp op) {
        compileExpression(op.left());
        compileExpression(op.right());

        Opcode opcode = tokenTypeToOpcode(op.operator());
        writer.writeOpcode(opcode);
    }

    private void compileUnaryOp(UnaryOp op) {
        compileExpression(op.operand());
        Opcode opcode = switch (op.operator()) {
            case MINUS -> Opcode.NEG;
            case NOT -> Opcode.NOT;
            default -> throw new CompileException("Unknown unary operator: " + op.operator());
        };
        writer.writeOpcode(opcode);
    }

    private void compileFunctionCall(FunctionCall call) {
        for (ExpressionNode arg : call.arguments()) compileExpression(arg);
        for (ExpressionNode opt : call.optionalArguments().values()) compileExpression(opt);

        writer.writeOpcode(Opcode.CALL);
        writer.writeString(call.fullyQualifiedName());
        writer.writeByte((byte) call.arguments().size());
        writer.writeByte((byte) call.optionalArguments().size());
        for (String key : call.optionalArguments().keySet()) writer.writeString(key);
    }

    private void compileIndexAccess(IndexAccess access) {
        compileExpression(access.target());
        compileExpression(access.index());
        writer.writeOpcode(Opcode.INDEX);
    }

    private void compileArrayLiteral(ArrayLiteral literal) {
        for(ExpressionNode element : literal.elements()) {
            compileExpression(element);
        }

        writer.writeOpcode(Opcode.PUSH);
        writer.writeByte((byte) PushType.ARRAY.ordinal());
        writer.writeShort((short) literal.elements().size());
    }

    private void compileMapLiteral(MapLiteral literal) {
        for(Map.Entry<String, ExpressionNode> entry : literal.entries().entrySet()) {
            compileExpression(entry.getValue());
            writer.writeOpcode(Opcode.PUSH);
            writer.writeByte((byte) PushType.STRING.ordinal());
            writer.writeString(entry.getKey());
        }
        writer.writeOpcode(Opcode.PUSH);
        writer.writeByte((byte) PushType.MAP.ordinal());
        writer.writeShort((short) literal.entries().size());
    }

    private void compileInterpolatedString(InterpolatedString string) {
        List<StringPart> parts = string.parts();

        if (parts.isEmpty()) {
            pushString("");
            return;
        }

        boolean first = true;
        for (StringPart part : parts) {
            if (part.isExpression()) {
                compileExpression(part.expression());
            } else {
                pushString(part.text());
            }

            if (!first) {
                writer.writeOpcode(Opcode.ADD);   // stack: [a, b] -> [str(a)+str(b)]
            }
            first = false;
        }

        // Sonderfall: nur ein Teil, der eine Expression ist ($"$x").
        // Ohne CONCAT bliebe der Rohwert (z.B. Integer) stehen.
        if (parts.size() == 1 && parts.get(0).isExpression()) {
            pushString("");
            writer.writeOpcode(Opcode.ADD);
        }
    }

    private void pushString(String s) {
        writer.writeOpcode(Opcode.PUSH);
        writer.writeByte((byte) PushType.STRING.ordinal());
        writer.writeString(s);
    }

    private void pushInt(int value) {
        writer.writeOpcode(Opcode.PUSH);
        writer.writeByte((byte) PushType.INTEGER.ordinal());
        writer.writeInt(value);
    }

    private Opcode tokenTypeToOpcode(TokenType tokenType) {
        return switch (tokenType) {
            case PLUS -> Opcode.ADD;
            case MINUS -> Opcode.SUB;
            case STAR -> Opcode.MUL;
            case SLASH -> Opcode.DIV;
            case EQUAL_EQUAL -> Opcode.EQ;
            case NOT_EQUAL -> Opcode.NEQ;
            case LESS -> Opcode.LT;
            case LESS_EQUAL -> Opcode.LTE;
            case GREATER -> Opcode.GT;
            case GREATER_EQUAL -> Opcode.GTE;
            case AND -> Opcode.AND;
            case OR -> Opcode.OR;
            case NOT -> Opcode.NOT;
            default -> throw new CompileException("Unknown token type: " + tokenType);
        };
    }

}
