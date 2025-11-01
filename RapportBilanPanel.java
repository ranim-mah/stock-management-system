import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.Font;

public class RapportBilanPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    private JComboBox<String> comboAnnee;
    private JButton btnGenerer, btnExporter;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblTotal;

    public RapportBilanPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setOpaque(false);

        JPanel glassPanel = new JPanel(new BorderLayout(10, 10));
        glassPanel.setBackground(BG_COLOR);
        glassPanel.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 2),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Panel de contrôle
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setOpaque(false);

        comboAnnee = new JComboBox<>();
        int currentYear = Year.now().getValue();
        for (int i = currentYear - 5; i <= currentYear; i++) comboAnnee.addItem(String.valueOf(i));
        comboAnnee.setSelectedItem(String.valueOf(currentYear));

        btnGenerer = createStyledButton("Générer Rapport");
        btnExporter = createStyledButton("Exporter en PDF");
        btnGenerer.addActionListener(e -> genererRapport());
        btnExporter.addActionListener(e -> exporterPDF());

        controlPanel.add(createStyledLabel("Année:"));
        controlPanel.add(comboAnnee);
        controlPanel.add(btnGenerer);
        controlPanel.add(btnExporter);

        // Tableau
        String[] columns = {"Local", "Article", "Type", "Catégorie", "Stock Initial", "Entrées", "Sorties", "Stock Final", "Valeur Totale"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column >= 4 ? Double.class : String.class;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
        table.getTableHeader().setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        table.setGridColor(MAIN_COLOR);

        // Alignement à droite pour les colonnes numériques
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 4; i < columns.length; i++) table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(MAIN_COLOR, 1));

        // Panel du total
        lblTotal = new JLabel("Total: 0.00 DT", SwingConstants.RIGHT);
        lblTotal.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        lblTotal.setForeground(MAIN_COLOR);

        glassPanel.add(controlPanel, BorderLayout.NORTH);
        glassPanel.add(scrollPane, BorderLayout.CENTER);
        glassPanel.add(lblTotal, BorderLayout.SOUTH);

        add(glassPanel, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(MAIN_COLOR);
        btn.setBorder(new EmptyBorder(10, 25, 10, 25));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(MAIN_COLOR.darker());
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(MAIN_COLOR);
            }
        });
        return btn;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        label.setForeground(MAIN_COLOR);
        return label;
    }

    private void genererRapport() {
        tableModel.setRowCount(0);
        String annee = (String) comboAnnee.getSelectedItem();
        double totalGeneral = 0.0;

        try (Connection conn = DatabaseConnector.getConnection()) {
            // 1. Récupérer tous les locaux
            Map<String, String> locaux = new HashMap<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT code_local, nom_local FROM LocalStockage")) {
                while (rs.next()) {
                    locaux.put(rs.getString("code_local"), rs.getString("nom_local"));
                }
            }

            // 2. Pour chaque local, calculer le bilan
            for (Map.Entry<String, String> entry : locaux.entrySet()) {
                String codeLocal = entry.getKey();
                String nomLocal = entry.getValue();

                // 2.1. Récupérer les articles du local
                try (PreparedStatement stmtArticles = conn.prepareStatement(
                        "SELECT reference, nom, type, categorie, prix_unitaire " +
                                "FROM Article WHERE code_local = ?")) {

                    stmtArticles.setString(1, codeLocal);
                    ResultSet rsArticles = stmtArticles.executeQuery();

                    while (rsArticles.next()) {
                        String reference = rsArticles.getString("reference");
                        String nomArticle = rsArticles.getString("nom");
                        String type = rsArticles.getString("type");
                        String categorie = rsArticles.getString("categorie");
                        double prixUnitaire = rsArticles.getDouble("prix_unitaire");

                        // 2.2. Calculer le stock initial (au 31/12 de l'année précédente)
                        double stockInitial = getStockInitial(conn, reference, Integer.parseInt(annee) - 1);

                        // 2.3. Calculer les entrées pour l'année sélectionnée
                        double entrees = getMouvements(conn, reference, annee, "ENTREE");

                        // 2.4. Calculer les sorties pour l'année sélectionnée
                        double sorties = getMouvements(conn, reference, annee, "SORTIE");

                        // 2.5. Calculer le stock final
                        double stockFinal = stockInitial + entrees - sorties;
                        double valeurTotale = stockFinal * prixUnitaire;
                        totalGeneral += valeurTotale;

                        // 2.6. Ajouter la ligne au tableau
                        tableModel.addRow(new Object[]{
                                nomLocal + " (" + codeLocal + ")",
                                reference + " - " + nomArticle,
                                type,
                                categorie,
                                stockInitial,
                                entrees,
                                sorties,
                                stockFinal,
                                valeurTotale
                        });
                    }
                }
            }

            lblTotal.setText(String.format("Total: %.2f DT", totalGeneral));
        } catch (SQLException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur de génération du rapport: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double getStockInitial(Connection conn, String reference, int annee) throws SQLException {
        String dateLimite = annee + "-12-31";

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COALESCE(SUM(CASE WHEN type_mouvement = 'ENTREE' THEN quantite ELSE -quantite END), 0) " +
                        "FROM MouvementStock " +
                        "WHERE reference_article = ? AND date_mouvement <= ?")) {

            stmt.setString(1, reference);
            stmt.setString(2, dateLimite);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private double getMouvements(Connection conn, String reference, String annee, String typeMouvement) throws SQLException {
        String dateDebut = annee + "-01-01";
        String dateFin = annee + "-12-31";

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COALESCE(SUM(quantite), 0) AS total " +
                        "FROM MouvementStock " +
                        "WHERE reference_article = ? " +
                        "AND type_mouvement = ? " +
                        "AND date_mouvement BETWEEN ? AND ?")) {

            stmt.setString(1, reference);
            stmt.setString(2, typeMouvement);
            stmt.setString(3, dateDebut);
            stmt.setString(4, dateFin);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0.0;
        }
    }

    private void exporterPDF() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Générez d'abord le rapport avant d'exporter",
                    "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Enregistrer le PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers PDF", "pdf"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".pdf")) {
                filePath += ".pdf";
            }

            try {
                Document document = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(document, new FileOutputStream(filePath));
                document.open();

                // Titre du document
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
                Paragraph title = new Paragraph("Bilan Annuel - Gestion de Stock ISIMM", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                // Informations de l'entête
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
                document.add(new Paragraph("Année: " + comboAnnee.getSelectedItem(), headerFont));
                document.add(new Paragraph("Date de génération: " + new java.util.Date(), headerFont));
                document.add(Chunk.NEWLINE);

                // Tableau des données
                PdfPTable pdfTable = new PdfPTable(table.getColumnCount());
                pdfTable.setWidthPercentage(100);

                // En-têtes de colonnes
                Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
                for (int i = 0; i < table.getColumnCount(); i++) {
                    PdfPCell cell = new PdfPCell(new Phrase(table.getColumnName(i), tableHeaderFont));
                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    pdfTable.addCell(cell);
                }

                // Données du tableau
                Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
                for (int i = 0; i < table.getRowCount(); i++) {
                    for (int j = 0; j < table.getColumnCount(); j++) {
                        Object value = table.getValueAt(i, j);
                        pdfTable.addCell(new Phrase(value != null ? value.toString() : "", dataFont));
                    }
                }

                document.add(pdfTable);
                document.add(Chunk.NEWLINE);

                // Total général
                Paragraph total = new Paragraph("Total Général: " + lblTotal.getText(), headerFont);
                total.setAlignment(Element.ALIGN_RIGHT);
                document.add(total);

                document.close();

                JOptionPane.showMessageDialog(this,
                        "PDF exporté avec succès!\nEmplacement: " + filePath,
                        "Export Réussi", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Erreur lors de l'export PDF: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}