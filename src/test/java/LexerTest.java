
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class LexerTest {
    @Test
    public void testLexer() {
        try(InputStream in = getClass().getResourceAsStream("test.ees")) {
            assertNotNull(in);
            String testString = new String(in.readAllBytes());
            Lexer lexer = new Lexer(testString);
            for(Token token : lexer.tokenize()) {
                System.out.print(token + " ");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
