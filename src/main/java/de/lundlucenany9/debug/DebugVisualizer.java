package de.lundlucenany9.debug;

import javax.swing.*;
import java.awt.*;

public class DebugVisualizer {
    private static final JFrame frame;
    private static final JEditorPane panel;
    private static final JTextArea output = new JTextArea();
    private static final JTextArea strings = new JTextArea();
    private static final JTextArea errors = new JTextArea();
    private static final JTextArea code = new JTextArea();

    static {
        frame = new JFrame("Debug");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        panel = new JEditorPane();
        panel.setEditable(false);

        output.setEditable(false);
        output.setColumns(30);

        strings.setEditable(false);
        strings.setColumns(30);

        errors.setEditable(false);
        errors.setColumns(20);

        code.setEditable(false);
        code.setColumns(30);
       // errors.setForeground(Color.RED);

        JScrollPane panelScrollPane = new JScrollPane(panel);
        JScrollPane outputScrollPane = new JScrollPane(output);
        JScrollPane stringsScrollPane = new JScrollPane(strings);
        JScrollPane errorsScrollPane = new JScrollPane(errors);
        JScrollPane codeScrollPane = new JScrollPane(code);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                panelScrollPane,
                outputScrollPane
        );
        JSplitPane stringsSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                splitPane,
                stringsScrollPane
        );

        JSplitPane errorSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                codeScrollPane,
                errorsScrollPane
        );

        JSplitPane mainSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                stringsSplitPane,
                errorSplitPane
        );

        splitPane.setResizeWeight(0.7);
        stringsSplitPane.setResizeWeight(0.7);
        errorSplitPane.setResizeWeight(0.5);
        mainSplitPane.setResizeWeight(0.7);
        errorSplitPane.setDividerLocation(0.5);

        frame.getContentPane().add(mainSplitPane, BorderLayout.CENTER);

    }

    public static void draw(String string) {
        SwingUtilities.invokeLater(() -> {
            panel.setText(panel.getText() + "\n" + string);
        });
    }

    public static void stack(String string) {
        SwingUtilities.invokeLater(() -> {
            output.append(string + "\n");
        });
    }
    public static void printsln(String string) {
        SwingUtilities.invokeLater(() -> {
            strings.append(string + "\n");
        });
    }
    public static void println(String string) {
        SwingUtilities.invokeLater(() -> {
            errors.append(string + "\n");
            errors.setCaretPosition(errors.getDocument().getLength());
        });
    }
    public static void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }
    public static void code(String string) {
        SwingUtilities.invokeLater(() -> code.setText(string));
    }

}