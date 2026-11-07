import de.lundlucenany9.ees.Lexer;
import de.lundlucenany9.ees.Parser;
import de.lundlucenany9.ees.compiler.Compiler;
import de.lundlucenany9.ees.ast.Program;
import de.lundlucenany9.ees.compiler.Disassembler;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FullCompilerTest {
    @Test
    public void testCompiler() {
        try(InputStream in = getClass().getResourceAsStream("test.ees")) {
            assertNotNull(in);
            String testString = new String(in.readAllBytes());
            Lexer lexer = new Lexer(testString);
            Program code  = new Parser(lexer.tokenize()).parse();
            byte[] bytecode = new Compiler().compile(code);
            System.out.println(Disassembler.disassemble(bytecode));
            System.out.println(HexFormat.ofDelimiter(" ").withUpperCase().formatHex(bytecode));
            try(OutputStream out = new FileOutputStream("test.eeb")) {
                out.write(bytecode);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
