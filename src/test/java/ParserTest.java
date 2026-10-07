import de.lundlucenany9.Lexer;
import de.lundlucenany9.Parser;
import de.lundlucenany9.ast.Program;
import de.lundlucenany9.ast.Statement;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ParserTest {
    @Test
    public void testParser() {
        try(InputStream in = getClass().getResourceAsStream("test.ees")) {
            assertNotNull(in);
            String testString = new String(in.readAllBytes());
            Lexer lexer = new Lexer(testString);
            Parser parser = new Parser(lexer.tokenize());
            Program program = parser.parse();
            for(Statement statement : program.getImports()) {
                System.out.println(statement);
            }
            for(Statement statement : program.getStatements()) {
                System.out.println(statement);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
