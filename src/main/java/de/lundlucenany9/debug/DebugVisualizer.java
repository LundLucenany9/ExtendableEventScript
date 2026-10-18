package de.lundlucenany9.debug;

import javax.swing.*;
import java.awt.*;

public class DebugVisualizer {
    private static final JFrame frame;
    private static final JEditorPane panel;
    private static final JTextArea output = new JTextArea();

    static {
        frame = new JFrame("Debug");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        panel = new JEditorPane();
        panel.setEditable(false);

        output.setEditable(false);
        output.setColumns(30);

        JScrollPane panelScrollPane = new JScrollPane(panel);
        JScrollPane outputScrollPane = new JScrollPane(output);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                panelScrollPane,
                outputScrollPane
        );

        splitPane.setResizeWeight(0.7);
        splitPane.setDividerLocation(550);

        frame.getContentPane().add(splitPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    public static void draw(String string) {
        SwingUtilities.invokeLater(() -> {
            panel.setText(panel.getText() + "\n" + string);
            panel.setCaretPosition(panel.getDocument().getLength());
        });
    }

    public static void println(String string) {
        SwingUtilities.invokeLater(() -> {
            output.append(string + "\n");
            output.setCaretPosition(output.getDocument().getLength());
        });
    }
}