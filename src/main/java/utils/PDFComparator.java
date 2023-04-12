package utils;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.JButton;


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
    private final JTextPane leftTextPane = new JTextPane();
    private final JTextPane rightTextPane = new JTextPane();
    private final JLabel leftFileLabel = new JLabel();
    private final JLabel rightFileLabel = new JLabel();

    public ComparatorFrame() {
        setTitle("PDF Text Comparator");
        setSize(800, 600);

        JPanel dashboardPanel = createDashboardPanel();
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(dashboardPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createDashboardPanel() {
        JPanel dashboardPanel = new JPanel(new GridBagLayout());
        JButton compareButton = new JButton("Compare PDF's");
        JButton italicButton = new JButton("Find Italic Text");

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        dashboardPanel.add(compareButton, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;

        // Add padding to the dashboard panel
        int panelPadding = 100;
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(panelPadding, panelPadding, panelPadding, panelPadding));

        // Add padding between the buttons
        int paddingBetweenButtons = 50; // Adjust this value to change the padding size between buttons
        constraints.insets = new Insets(0, paddingBetweenButtons, 0, 0);
        dashboardPanel.add(italicButton, constraints);

        // Add ActionListener to the compareButton
        compareButton.addActionListener(event -> {
            // Hide the dashboard panel
            dashboardPanel.setVisible(false);

            // Show the text panel and menu
            JPanel mainPanel = (JPanel) getContentPane();
            JPanel pdfComparePanel = createPDFComparePanel();
            mainPanel.add(pdfComparePanel, BorderLayout.CENTER);
            mainPanel.revalidate();
            mainPanel.repaint();

            JMenuBar menuBar = createMenuBar();
            setJMenuBar(menuBar);
            menuBar.setVisible(true);
        });

        return dashboardPanel;
    }

    private JPanel createPDFComparePanel() {
        leftTextPane.setContentType("text/html");
        rightTextPane.setContentType("text/html");
        JPanel textPanel = new JPanel(new GridLayout(1, 2));
        textPanel.add(createScrollPaneWithLabel(leftTextPane, leftFileLabel));
        textPanel.add(createScrollPaneWithLabel(rightTextPane, rightFileLabel));

        return textPanel;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
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

        JMenuItem returnToDashboardMenuItem = new JMenuItem("Return to Dashboard");
        returnToDashboardMenuItem.addActionListener(event -> {
            // Remove the text panel and menu
            JPanel mainPanel = (JPanel) getContentPane();
            mainPanel.removeAll();
            // Show the dashboard panel
            JPanel dashboardPanel = createDashboardPanel();
            mainPanel.add(dashboardPanel, BorderLayout.CENTER);
            mainPanel.revalidate();
            mainPanel.repaint();
            // Hide the menu bar
            setJMenuBar(null);
        });

        fileMenu.add(returnToDashboardMenuItem);

        return menuBar;
    }

    private JPanel createScrollPaneWithLabel(JTextPane textPane, JLabel label) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.NORTH);
        panel.add(new JScrollPane(textPane), BorderLayout.CENTER);
        return panel;
    }

    private void comparePDFs(File file1, File file2) throws IOException {
        try (PDDocument doc1 = PDDocument.load(file1); PDDocument doc2 = PDDocument.load(file2)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text1 = stripper.getText(doc1);
            String text2 = stripper.getText(doc2);

            DiffMatchPatch dmp = new DiffMatchPatch();
            LinkedList<Diff> diffs = dmp.diff_main(text1, text2);
            dmp.diffCleanupSemantic(diffs);

            leftFileLabel.setText(file1.getName());
            rightFileLabel.setText(file2.getName());
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
                        result.append("<span style='background-color: #f5424b'>");
                        result.append(diff.text);
                        result.append("</span>");
                    }
                    break;
                case DELETE:
                    if (left) {
                        result.append("<span style='background-color: #f5424b;'>");
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
