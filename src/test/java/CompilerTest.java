import de.lundlucenany9.Lexer;
import de.lundlucenany9.Parser;
import de.lundlucenany9.compiler.Compiler;
import de.lundlucenany9.compiler.Disassembler;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompilerTest {
    private String compile(String source) {
        var tokens = new Lexer(source).tokenize();
        var program = new Parser(tokens).parse();
        byte[] code = new Compiler().compile(program);
        System.out.println(HexFormat.ofDelimiter(" ").withUpperCase().formatHex(code));
        return Disassembler.disassemble(code);
    }

    @Test
    void varDeclaration() {

        assertEquals("""
            ; version 1
            0005  PUSH INTEGER 10
            0011  DEFINE_VAR x
            """, compile("var x = 10\n"));
    }
    @Test
    void roVarDeclaration() {

        assertEquals("""
            ; version 1
            0005  PUSH INTEGER 10
            0011  DEFINE_VAR readonly x
            """, compile("rovar x = 10\n"));
    }
    @Test
    void globalVarDeclaration() {

        assertEquals("""
            ; version 1
            0005  PUSH INTEGER 10
            0011  DEFINE_GLOBAL_VAR x
            """, compile("global var x = 10\n"));
    }
    @Test
    void readVar() {

        assertEquals("""
            ; version 1
            0005  PUSH INTEGER 10
            0011  DEFINE_VAR x
            0016  LOAD x
            0020  POP
            """, compile("var x = 10\n$x"));
    }
}