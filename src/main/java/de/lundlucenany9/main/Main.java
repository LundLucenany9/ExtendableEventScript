package de.lundlucenany9.main;


import de.lundlucenany9.Lexer;
import de.lundlucenany9.Parser;
import de.lundlucenany9.Token;
import de.lundlucenany9.ast.Program;
import de.lundlucenany9.compiler.Compiler;
import de.lundlucenany9.compiler.Disassembler;
import de.lundlucenany9.debug.DebugVisualizer;
import de.lundlucenany9.runtime.VM;
import de.lundlucenany9.runtime.simple.SimpleHost;

import java.io.*;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("No arguments given");
            printUsage();
            System.exit(1);
        }
        if(args[0].equals("-c") || args[0].equals("--compile")) {
            String file = args[1];
            String output = "script.eeb";
            if(args.length > 2) {
                if(args[2].equals("-o") || args[2].equals("--output")) {
                    output = args[3];
                }
            }
            try(InputStream in = new FileInputStream(file)) {
                String code = new String(in.readAllBytes());
                System.out.println("Lexing '" + file + "' ...");
                List<Token> tokens = new Lexer(code).tokenize();
                System.out.println("Parsing...");
                Program program = new Parser(tokens).parse();
                System.out.println("Compiling...");
                byte[] executable = new Compiler().compile(program);
                try(OutputStream out = new FileOutputStream(output)) {
                    out.write(executable);
                    System.out.println("Compiled to " + output);
                }
            }catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        if(args[0].equals("-e") || args[0].equals("--execute")) {
            String arg = args[1];
            boolean debug = false;
            if(arg.equals("-d") || arg.equals("--debug")) {
                debug = true;
                arg = args[2];
                DebugVisualizer.show();
                if(arg == null) {
                    System.out.println("No file given");
                    printUsage();
                    System.exit(1);
                }
            }
            try(InputStream in = new FileInputStream(arg)) {
                VM vm = new VM(SimpleHost.getStandardHost());
                try {
                    byte[] code = in.readAllBytes();
                    if(debug) {
                        DebugVisualizer.code(Disassembler.disassemble(code));
                    }
                    vm.run(code, new HashMap<>(), new HashMap<>());
                }catch (Exception e) {
                    if(debug) {
                        DebugVisualizer.println(e.getMessage());
                    } else{
                        System.out.println("An error occurred: " + e.getMessage());
                    }
                }
                if(debug)
                    while (true);
            }catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static void printUsage() {
        System.out.println("""
                USAGE:
                  java -jar EEScript.jar
                 \s
                  Arguments:
                    -c | --compile <file> Compile script
                      -o | --output <file> Output file (default: script.eeb)
                    -e | --execute [<script>] Execute script
                      -d | --debug <script> Execute script and show debug information
               """);
    }
}
