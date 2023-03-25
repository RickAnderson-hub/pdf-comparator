package utils;

import java.awt.*;
import java.awt.event.*;
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
    private final JTextArea leftTextArea;
    private final JTextArea rightTextArea;

    public ComparatorFrame() {
        setTitle("PDF Comparator");
        setSize(800, 600);

        leftTextArea = new JTextArea();
        rightTextArea = new JTextArea();

        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(new JScrollPane(leftTextArea));
        panel.add(new JScrollPane(rightTextArea));
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
//            dmp.diff_cleanupSemantic(diffs);

            leftTextArea.setText(addLineNumbers(formatDifferences(diffs, true)));
            rightTextArea.setText(addLineNumbers(formatDifferences(diffs, false)));
        }
    }

        private String formatDifferences(List<Diff> diffs, boolean left) {
            StringBuilder result = new StringBuilder();
            for (Diff diff : diffs) {
                switch (diff.operation) {
                    case EQUAL:
                        result.append(diff.text);
                        break;
                    case INSERT:
                        if (!left) {
                            result.append("\033[32m"); // Set text color to green
                            result.append(diff.text);
                            result.append("\033[0m"); // Reset text color
                        }
                        break;
                    case DELETE:
                        if (left) {
                            result.append("\033[31m"); // Set text color to red
                            result.append(diff.text);
                            result.append("\033[0m"); // Reset text color
                        }
                        break;
                }
            }
            return result.toString();
    }

    private String addLineNumbers(String text) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            result.append(String.format("%d: %s%n", i + 1, lines[i]));
        }
        return result.toString();
    }
}
