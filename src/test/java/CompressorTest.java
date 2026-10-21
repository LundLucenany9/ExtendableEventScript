import de.lundlucenany9.compiler.Compiler;
import de.lundlucenany9.Compressor;
import de.lundlucenany9.runtime.VM;
import de.lundlucenany9.runtime.simple.SimpleHost;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class CompressorTest {
    @Test
    void compressorTest() throws IOException {
        String code = """
                import std
                import std.io
                
                log "Hello World"
                for rovar i -> 0-10
                    log $"i is $($i + 1)"
                log "Reverse!!"
                for rovar i -> 5-0
                    log $i
                log "i is " {newline = false}
                log $i
                if 5 == 6
                    log "5 is 6"
                elif 6 == 3
                    log "6 is not 3"
                else
                    log "5 is not 6"
                if 5 == 5
                    log "if"
                log $"i is $i"
                log (["a", "b", "c"] + "d")[3]
                log ({"a"=1, "b"=2,"c"=3}["c"])
                function add(a,b)
                    log $"a is $a, b is $b"
                    return $a + $b
                add 1 2
                log add 5 100
                log #i["defined"]
                rovar x = 1
                log ({"a"={"b"="hi","c"=[add 1 2,"b"]}})["a"]["c"][0]{newline=false}
                logf "lex %.2f ms, parse %.2f\\n" ([1.0, 2.0])
                log $"hi\\nlol"
                var name = input "Enter your name: "
                writeln $"Hello, $name!"
                """;
        VM vm = new VM(SimpleHost.getStandardHost());
        vm.run(Compressor.compress(new Compiler().compile(code)), new HashMap<>(), new HashMap<>());
        try(OutputStream os = new FileOutputStream("test.eeb")) {
            os.write(Compressor.compress(new Compiler().compile(code)));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
