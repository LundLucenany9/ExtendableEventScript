package de.lundlucenany9.runtime.simple;

import java.util.Scanner;

public class StandardIO implements Library{
    private static final Scanner scanner = new Scanner(System.in);

    @Override
    public String getName() {
        return "std.io";
    }

    @EESFunction
    String read() {
        return scanner.nextLine();
    }
    @EESFunction
    void write(String message) {
        System.out.print(message);
    }
    @EESFunction
    void writeln(String message) {
        System.out.println(message);
    }
    @EESFunction
    void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    @EESFunction
    String input(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
}
