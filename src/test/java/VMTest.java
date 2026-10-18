import de.lundlucenany9.Lexer;
import de.lundlucenany9.Parser;
import de.lundlucenany9.Token;
import de.lundlucenany9.ast.Program;
import de.lundlucenany9.compiler.Compiler;
import de.lundlucenany9.debug.DebugVisualizer;
import de.lundlucenany9.runtime.Host;
import de.lundlucenany9.runtime.StaticObjects;
import de.lundlucenany9.runtime.VM;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;


public class VMTest {
    @Test
    public void testVM() {
        Host host = (name, args, opts) ->
            {
                if(name.equals("print")) {
                    if(opts.containsKey("newline") && !(boolean) opts.get("newline"))
                        //System.out.print(args.get(0));
                        DebugVisualizer.println(args.get(0).toString());
                    else
                        DebugVisualizer.println(args.get(0).toString());
                    return StaticObjects.UNDEFINED;
                }
                return StaticObjects.UNDEFINED;
            };
        String source = """
                print "Hello World"
                for rovar i -> 0-10
                    print $"i is $($i + 1)"
                print "Reverse!!"
                for rovar i -> 5-0
                    print $i
                print "i is " {newline = false}
                print $i
                if 5 == 6
                    print "5 is 6"
                elif 6 == 3
                    print "6 is not 3"
                else
                    print "5 is not 6"
                if 5 == 5
                    print "if"
                print $"i is $i"
                print (["a", "b", "c"] + "d")[3]
                print ({"a"=1, "b"=2,"c"=3}["c"])
                function add(a,b)
                    print $"a is $a, b is $b"
                    return $a + $b
                add 1 2
                print (add 5 100)
                """;

        DebugVisualizer.println("Starting...");
        long t0 = System.nanoTime();
        List<Token> tokens = new Lexer(source).tokenize();
        long t1 = System.nanoTime();
        Program program = new Parser(tokens).parse();
        long t2 = System.nanoTime();
        byte[] code = new Compiler().compile(program);
        long t3 = System.nanoTime();

        //System.out.println(HexFormat.ofDelimiter(" ").formatHex(code, 430, 450));
        //System.out.println(Disassembler.disassemble(code));

        VM vm = new VM(host);
        VM silentVM = new VM((n,a,o)-> StaticObjects.UNDEFINED);

        //for (int i = 0; i < 1000; i++) silentVM.run(code, new HashMap<>(), new HashMap<>());
        long t4 = System.nanoTime();
        //for (int i = 0; i < 1000; i++) silentVM.run(code);
        vm.run(code, new HashMap<>(), new HashMap<>());
        long t5 = System.nanoTime();
        System.out.printf("lex %.2f ms, parse %.2f ms, compile %.2f ms, vm %.4f ms/run%n",
                (t1-t0)/1e6, (t2-t1)/1e6, (t3-t2)/1e6, (t5-t4)/1e6);
    }
}
