package utils;

import java.awt.*;
import java.io.*;
import javax.swing.*;

import fun.mike.dmp.Diff;
import fun.mike.dmp.DiffMatchPatch;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;

import java.util.LinkedList;
import java.util.List;

public class PDFComparator {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new ComparatorFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}

class ComparatorFrame extends JFrame {
    private final JTextPane leftTextPane;
    private final JTextPane rightTextPane;

    public ComparatorFrame() {
        setTitle("PDFText Comparator");
        setSize(800, 600);

        leftTextPane = new JTextPane();
        rightTextPane = new JTextPane();

        leftTextPane.setContentType("text/html");
        rightTextPane.setContentType("text/html");

        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(new JScrollPane(leftTextPane));
        panel.add(new JScrollPane(rightTextPane));
        add(panel);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem openMenuItem = new JMenuItem("Open PDF Files");
        openMenuItem.addActionListener(event -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                if (selectedFiles.length == 2) {
                    try {
                        comparePDFs(selectedFiles[0], selectedFiles[1]);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(this, "Error reading PDF files", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Please select two PDF files", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        fileMenu.add(openMenuItem);
    }

    private void comparePDFs(File file1, File file2) throws IOException {
        try (PDDocument doc1 = PDDocument.load(file1); PDDocument doc2 = PDDocument.load(file2)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text1 = stripper.getText(doc1);
            String text2 = stripper.getText(doc2);

            DiffMatchPatch dmp = new DiffMatchPatch();
            LinkedList<Diff> diffs = dmp.diff_main(text1, text2);
            dmp.diffCleanupSemantic(diffs);

            leftTextPane.setText(addLineNumbers(formatDifferences(diffs, true)));
            rightTextPane.setText(addLineNumbers(formatDifferences(diffs, false)));
        }
    }

        private String formatDifferences(List<Diff> diffs, boolean left) {
            StringBuilder result = new StringBuilder();
            result.append("<html><body>");

            for (Diff diff : diffs) {
                switch (diff.operation) {
                    case EQUAL:
                        result.append(diff.text);
                        break;
                    case INSERT:
                        if (!left) {
                            result.append("<span style='background-color: #ccffcc;'>");
                            result.append(diff.text);
                            result.append("</span>");
                        }
                        break;
                    case DELETE:
                        if (left) {
                            result.append("<span style='background-color: #ffcccc;'>");
                            result.append(diff.text);
                            result.append("</span>");
                        }
                        break;
                }
            }

            result.append("</body></html>");
            return result.toString();
    }

    private String addLineNumbers(String text) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            result.append("<br>");
            result.append(String.format("%d: %s%n", i + 1, lines[i]));
            result.append("</br>");
        }
        return result.toString();
    }
}
