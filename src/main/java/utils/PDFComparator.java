package utils;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;

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

            leftTextArea.setText(addLineNumbers(text1));
            rightTextArea.setText(addLineNumbers(text2));

            // Additional code to highlight differences can be added here.
            // Consider using a library like google-diff-match-patch (https://github.com/google/diff-match-patch)
            // to compare the texts and highlight the differences.
        }
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
