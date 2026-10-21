package de.lundlucenany9.main;


import de.lundlucenany9.Compressor;
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
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        if (args[0].equals("-h") || args[0].equals("--help")) {
            printUsage();
            return;
        }

        if (args.length < 2) {
            System.out.println("Missing file argument");
            printUsage();
            System.exit(1);
        }
        switch (args[0]) {
            case "-c", "--compile" -> {
                String file = args[1];
                String output = "script.eex";
                if (args.length > 2) {
                    if (args[2].equals("-o") || args[2].equals("--output")) {
                        if (args.length < 4) {
                            System.out.println("Missing output file");
                            printUsage();
                            System.exit(1);
                        }
                        output = args[3];
                    } else {
                        System.out.println("Unknown argument: " + args[2]);
                        printUsage();
                        System.exit(1);
                    }
                }
                try (InputStream in = new FileInputStream(file)) {
                    String code = new String(in.readAllBytes());
                    System.out.println("Lexing '" + file + "' ...");
                    List<Token> tokens = new Lexer(code).tokenize();
                    System.out.println("Parsing...");
                    Program program = new Parser(tokens).parse();
                    System.out.println("Compiling...");
                    byte[] executable = new Compiler().compile(program);
                    System.out.println("Compressing...");
                    byte[] compressed = Compressor.compress(executable);
                    try (OutputStream out = new FileOutputStream(output)) {
                        out.write(compressed);
                        System.out.println("Compiled to " + output);
                    }
                } catch (Exception e) {
                    System.err.println("An error occurred: " + e.getMessage());
                    System.exit(1);
                }
            }
            case "-C", "--compile-only" -> {
                String file = args[1];
                String output = "script.eeb";
                if (args.length > 2) {
                    if (args[2].equals("-o") || args[2].equals("--output")) {
                        if (args.length < 4) {
                            System.out.println("Missing output file");
                            printUsage();
                            System.exit(1);
                        }
                        output = args[3];
                    } else {
                        System.out.println("Unknown argument: " + args[2]);
                        printUsage();
                        System.exit(1);
                    }
                }
                try (InputStream in = new FileInputStream(file)) {
                    String code = new String(in.readAllBytes());
                    System.out.println("Lexing '" + file + "' ...");
                    List<Token> tokens = new Lexer(code).tokenize();
                    System.out.println("Parsing...");
                    Program program = new Parser(tokens).parse();
                    System.out.println("Compiling...");
                    byte[] executable = new Compiler().compile(program);
                    try (OutputStream out = new FileOutputStream(output)) {
                        out.write(executable);
                        System.out.println("Compiled to " + output);
                    }
                } catch (Exception e) {
                    System.err.println("An error occurred: " + e.getMessage());
                    System.exit(1);
                }
            }
            case "-e", "--execute" -> {
                String file = args[1];
                if (file == null) {
                    System.out.println("No file given");
                    printUsage();
                    System.exit(1);
                }

                try (InputStream in = new FileInputStream(file)) {
                    VM vm = new VM(SimpleHost.getStandardHost());
                    try {
                        byte[] code = in.readAllBytes();
                        vm.run(code, new HashMap<>(), new HashMap<>());
                    } catch (Exception e) {
                        System.err.println("An error occurred: " + e.getMessage());
                        System.exit(1);
                    }
                } catch (Exception e) {
                    System.err.println("An error occurred: " + e.getMessage());
                    System.exit(1);
                }
            }
            case "-d", "--debug" -> {
                String file = args[1];
                System.out.println("Opening debugger...");
                DebugVisualizer.show();

                try (InputStream in = new FileInputStream(file)) {
                    System.out.println("Reading file...");
                    byte[] code = in.readAllBytes();

                    if (file.endsWith(".eex")) {
                        System.out.println("Decompressing...");
                        code = Compressor.decompress(code);
                    }

                    System.out.println("Disassembling...");
                    DebugVisualizer.code(Disassembler.disassemble(code));

                    System.out.println("Running VM...");
                    VM vm = new VM(SimpleHost.getStandardHost());
                    vm.run(code, new HashMap<>(), new HashMap<>());

                    System.out.println("VM finished.");
                } catch (Exception e) {
                    e.printStackTrace();
                    DebugVisualizer.println(e.toString());
                }
            }
            default -> {
                System.out.println("Invalid option: " + args[0]);
                printUsage();
            }
        }
    }

    private static void printUsage() {
        System.out.println("""
            USAGE:
              java -jar EEScript.jar <option> [arguments]

            Options:
              -c, --compile <file>         Compile script and compress
                -o, --output <file>        Output file (default: script.eex)
              -C, --compile-only <file>    Compile script without compressing
                -o, --output <file>        Output file (default: script.eeb)
              -e, --execute <script>       Execute script
              -d, --debug <script>         Execute script with debug information

              -h, --help                   Show this help message
            """);
    }
}
