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
    private final JTextPane leftTextPane;
    private final JTextPane rightTextPane;
    private final JLabel leftFileLabel;
    private final JLabel rightFileLabel;

    public ComparatorFrame() {
        setTitle("PDF Text Comparator");
        setSize(800, 600);

        // create the dashboard panel with two buttons
        JPanel dashboardPanel = new JPanel(new GridLayout(1, 2));
        JButton compareButton = new JButton("Compare PDF's");
        JButton italicButton = new JButton("Find Italic Text");
        dashboardPanel.add(compareButton);
        dashboardPanel.add(italicButton);

        // create the text panes and file labels
        leftTextPane = new JTextPane();
        rightTextPane = new JTextPane();

        leftTextPane.setContentType("text/html");
        rightTextPane.setContentType("text/html");

        leftFileLabel = new JLabel();
        rightFileLabel = new JLabel();

        // create the main panel with the dashboard panel and the text panels
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(dashboardPanel, BorderLayout.NORTH);
        JPanel textPanel = new JPanel(new GridLayout(1, 2));
        textPanel.add(createScrollPaneWithLabel(leftTextPane, leftFileLabel));
        textPanel.add(createScrollPaneWithLabel(rightTextPane, rightFileLabel));
        mainPanel.add(textPanel, BorderLayout.CENTER);
        add(mainPanel);

        // create the file menu
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

        // hide the menu bar on the dashboard
        menuBar.setVisible(false);

        // add listener to the "Compare PDF's" button to open a new window with a visible menu bar
        compareButton.addActionListener(event -> {
            // hide the dashboard frame
            setVisible(false);

            // create new frame
            JFrame compareFrame = new JFrame("Compare PDF's");
            compareFrame.setSize(800, 600);

            // add menu bar to new frame
            JMenuBar compareMenuBar = new JMenuBar();
            compareFrame.setJMenuBar(compareMenuBar);

            // create "Return to Dashboard" menu item
            JMenuItem dashboardMenuItem = new JMenuItem("Return to Dashboard");
            dashboardMenuItem.addActionListener(e -> {
                compareFrame.dispose();
                setVisible(true);
            });

            // create "Compare PDF's" menu item (disabled)
            JMenuItem compareMenuItem = new JMenuItem("Compare PDF's");
            compareMenuItem.setEnabled(false);

            // add menu items to menu bar
            JMenu compareMenu = new JMenu("File");
            compareMenu.add(dashboardMenuItem);
            compareMenu.add(compareMenuItem);
            compareMenuBar.add(compareMenu);

            compareFrame.setVisible(true);
        });

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
