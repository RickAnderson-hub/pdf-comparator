package utils;

import java.awt.*;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.JButton;
import javax.swing.filechooser.FileNameExtensionFilter;


import fun.mike.dmp.Diff;
import fun.mike.dmp.DiffMatchPatch;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

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
    private final JTextPane singleTextPane = new JTextPane();
    private final JLabel singlePaneLabel = new JLabel();
    private final JLabel leftFileLabel = new JLabel();
    private final JLabel rightFileLabel = new JLabel();
    private final String openingTag = "<span style='background-color: #FFA500'>";
    private final String closingTag = "</span>";
    private Image backgroundImage;

    public ComparatorFrame() {
        try {
            backgroundImage = ImageIO.read(getClass().getResourceAsStream("/wallpaper.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setTitle("Smarties PDF Tools");
        setSize(800, 600);
        JPanel dashboardPanel = createDashboardPanel();
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(dashboardPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    private String loadAppVersion() {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("version.properties")) {
            properties.load(inputStream);
            return properties.getProperty("app.version");
        } catch (IOException e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    private JPanel createDashboardPanel() {
        String appVersion = loadAppVersion();
        JPanel dashboardPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
                }
                int paddingRight = 10;
                int paddingBottom = 10;
                int textWidth = g.getFontMetrics().stringWidth("ver " + appVersion);
                int textHeight = g.getFontMetrics().getHeight();
                int rectX = this.getWidth() - textWidth - paddingRight;
                int rectY = this.getHeight() - textHeight - paddingBottom;
                g.setColor(Color.WHITE);
                g.fillRect(rectX, rectY, textWidth, textHeight);
                g.setColor(Color.BLACK);
                int textY = rectY + (textHeight - g.getFontMetrics().getAscent()) / 2 + g.getFontMetrics().getAscent();
                g.drawString("ver " + appVersion, rectX, textY);
            }
        };
        JButton compareButton = new JButton("Compare PDF's");
        JButton italicButton = new JButton("Find italic Text");
        GridBagConstraints constraints = setGridBag(dashboardPanel, compareButton);
        dashboardPanel.add(italicButton, constraints);
        compareButton.addActionListener(event -> showComparePdfPanel(dashboardPanel));
        italicButton.addActionListener(event -> showItalicPdfPanel(dashboardPanel));
        return dashboardPanel;
    }

    private void showItalicPdfPanel(JPanel dashboardPanel) {
        dashboardPanel.setVisible(false);
        JPanel mainPanel = (JPanel) getContentPane();
        JPanel pdfComparePanel = createItalicPdfPanel();
        mainPanel.add(pdfComparePanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        JMenuBar menuBar = createItalicMenuBar();
        setJMenuBar(menuBar);
        menuBar.setVisible(true);
        openSinglePDF();
    }

    private GridBagConstraints setGridBag(JPanel dashboardPanel, JButton button) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        dashboardPanel.add(button, constraints);
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        int panelPadding = 100;
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(panelPadding, panelPadding, panelPadding, panelPadding));
        int paddingBetweenButtons = 50;
        constraints.insets = new Insets(0, paddingBetweenButtons, 0, 0);
        return constraints;
    }

    private void showComparePdfPanel(JPanel dashboardPanel) {
        dashboardPanel.setVisible(false);
        JPanel mainPanel = (JPanel) getContentPane();
        JPanel pdfComparePanel = createPDFComparePanel();
        mainPanel.add(pdfComparePanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        JMenuBar menuBar = createPdfMenuBar();
        setJMenuBar(menuBar);
        menuBar.setVisible(true);
    }

    private void openSinglePDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files", "pdf");
        fileChooser.setFileFilter(pdfFilter);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (PDDocument doc = PDDocument.load(selectedFile)) {
                PDFTextStripper stripper = new PDFTextStripper() {
                    private int lineNumber = 1;

                    @Override
                    protected void writeLineSeparator() throws IOException {
                        writeString("<br>" + lineNumber + ": </br>");
                        lineNumber++;
                        super.writeLineSeparator();
                    }

                    @Override
                    protected void processTextPosition(TextPosition text) {
                        PDFont font = text.getFont();
                        PDFontDescriptor descriptor = font.getFontDescriptor();
                        boolean isItalic = descriptor != null && descriptor.isItalic();
                        if (isItalic) {
                            try {
                                writeString(openingTag);
                                writeString(text.toString());
                                writeString(closingTag);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            super.processTextPosition(text);
                        }
                    }
                };
                String pdfText = stripper.getText(doc);
                singlePaneLabel.setText(selectedFile.getName());
                singleTextPane.setContentType("text/html");
                singleTextPane.setText("<html><body>" + pdfText + "</body></html>");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading PDF file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createPDFComparePanel() {
        leftTextPane.setContentType("text/html");
        rightTextPane.setContentType("text/html");
        JPanel textPanel = new JPanel(new GridLayout(1, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
                }
            }
        };
        textPanel.add(createScrollPaneWithLabel(leftTextPane, leftFileLabel));
        textPanel.add(createScrollPaneWithLabel(rightTextPane, rightFileLabel));
        return textPanel;
    }

    private JPanel createItalicPdfPanel() {
        singleTextPane.setContentType("text/html");
        JPanel textPanel = new JPanel(new GridLayout(1, 1)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
                }
            }
        };
        textPanel.add(createScrollPaneWithLabel(singleTextPane, singlePaneLabel));
        return textPanel;
    }

    private JMenuBar createPdfMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openMenuItem = new JMenuItem("Open PDF Files");
        openMenuItem.addActionListener(event -> choose2Pdfs());
        fileMenu.add(openMenuItem);
        JMenuItem returnToDashboardMenuItem = new JMenuItem("Return to Dashboard");
        returnToDashboardMenuItem.addActionListener(event -> showDashboard());
        fileMenu.add(returnToDashboardMenuItem);
        return menuBar;
    }

    private JMenuBar createItalicMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Menu");
        menuBar.add(fileMenu);
        JMenuItem italicMenuItem = new JMenuItem("Check for italics");
        italicMenuItem.addActionListener(event -> openSinglePDF());
        fileMenu.add(italicMenuItem);
        JMenuItem returnToDashboardMenuItem = new JMenuItem("Return to Dashboard");
        returnToDashboardMenuItem.addActionListener(event -> showDashboard());
        fileMenu.add(returnToDashboardMenuItem);
        return menuBar;
    }

    private void showDashboard() {
        JPanel mainPanel = (JPanel) getContentPane();
        mainPanel.removeAll();
        JPanel dashboardPanel = createDashboardPanel();
        mainPanel.add(dashboardPanel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        setJMenuBar(null);
    }

    private void choose2Pdfs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files", "pdf");
        fileChooser.setFileFilter(pdfFilter);
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
            leftTextPane.setText(addLineNumbers(formatPdfContentDifferences(diffs, true)));
            rightTextPane.setText(addLineNumbers(formatPdfContentDifferences(diffs, false)));
        }
    }

    private String formatPdfContentDifferences(List<Diff> diffs, boolean left) {
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
