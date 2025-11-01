import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

public class RapportStatsPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    private JComboBox<String> comboTypeStats;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnGenerer;

    public RapportStatsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setOpaque(false);

        JPanel glassPanel = new JPanel(new BorderLayout(10, 10));
        glassPanel.setBackground(BG_COLOR);
        glassPanel.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 2),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Panel de sélection
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        selectionPanel.setOpaque(false);

        comboTypeStats = new JComboBox<>(new String[]{
                "Articles les plus consommés",
                "Articles en rupture de stock",
                "Articles proches de péremption",
                "Fournisseurs les plus utilisés",
                "Services les plus demandeurs"
        });

        btnGenerer = createStyledButton("Générer Statistiques");
        btnGenerer.addActionListener(e -> genererStats());

        selectionPanel.add(createStyledLabel("Type de statistiques:"));
        selectionPanel.add(comboTypeStats);
        selectionPanel.add(btnGenerer);

        // Tableau
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.setGridColor(MAIN_COLOR);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(MAIN_COLOR, 1));

        glassPanel.add(selectionPanel, BorderLayout.NORTH);
        glassPanel.add(scrollPane, BorderLayout.CENTER);

        add(glassPanel, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
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
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(MAIN_COLOR);
        return label;
    }

    private void genererStats() {
        String typeStats = (String) comboTypeStats.getSelectedItem();
        tableModel.setRowCount(0);

        try (Connection conn = DatabaseConnector.getConnection()) {
            switch (typeStats) {
                case "Articles les plus consommés":
                    genererStatsArticlesConsommes(conn);
                    break;
                case "Articles en rupture de stock":
                    genererStatsRuptureStock(conn);
                    break;
                case "Articles proches de péremption":
                    genererStatsPeremption(conn);
                    break;
                case "Fournisseurs les plus utilisés":
                    genererStatsFournisseurs(conn);
                    break;
                case "Services les plus demandeurs":
                    genererStatsServices(conn);
                    break;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de génération: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void genererStatsArticlesConsommes(Connection conn) throws SQLException {
        String[] columns = {"Article", "Référence", "Quantité consommée", "Unité"};
        tableModel.setColumnIdentifiers(columns);

        String sql = "SELECT a.reference, a.nom, SUM(lci.quantite) AS total " +
                "FROM LigneCommandeInterne lci " +
                "JOIN Article a ON lci.reference_article = a.reference " +
                "GROUP BY a.reference, a.nom " +
                "ORDER BY total DESC " +
                "LIMIT 20";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("nom"),
                        rs.getString("reference"),
                        rs.getInt("total"),
                        "unités"
                });
            }
        }
    }

    private void genererStatsRuptureStock(Connection conn) throws SQLException {
        String[] columns = {"Article", "Référence", "Stock actuel", "Stock minimal", "Local"};
        tableModel.setColumnIdentifiers(columns);

        String sql = "SELECT a.reference, a.nom, a.quantite, a.seuil_alerte, l.nom_local " +
                "FROM Article a JOIN LocalStockage l ON a.code_local = l.code_local " +
                "WHERE a.quantite <= a.seuil_alerte " +
                "ORDER BY a.quantite ASC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("nom"),
                        rs.getString("reference"),
                        rs.getInt("quantite"),
                        rs.getInt("seuil_alerte"),
                        rs.getString("nom_local")
                });
            }
        }
    }

    private void genererStatsPeremption(Connection conn) throws SQLException {
        String[] columns = {"Article", "Référence", "Date péremption", "Jours restants", "Local"};
        tableModel.setColumnIdentifiers(columns);

        String sql = "SELECT a.reference, a.nom, a.date_peremption, " +
                "DATEDIFF(a.date_peremption, CURDATE()) AS jours_restants, l.nom_local " +
                "FROM Article a JOIN LocalStockage l ON a.code_local = l.code_local " +
                "WHERE a.date_peremption IS NOT NULL " +
                "AND a.date_peremption BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
                "ORDER BY a.date_peremption ASC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("nom"),
                        rs.getString("reference"),
                        rs.getDate("date_peremption"),
                        rs.getInt("jours_restants"),
                        rs.getString("nom_local")
                });
            }
        }
    }

    private void genererStatsFournisseurs(Connection conn) throws SQLException {
        String[] columns = {"Fournisseur", "Nombre d'articles", "Commandes", "Montant total"};
        tableModel.setColumnIdentifiers(columns);

        String sql = "SELECT f.nom, COUNT(DISTINCT a.reference) AS nb_articles, " +
                "COUNT(DISTINCT ce.id_commande) AS nb_commandes, " +
                "SUM(ce.montant_total) AS montant_total " +
                "FROM Fournisseur f " +
                "LEFT JOIN Article a ON f.id_fournisseur = a.id_fournisseur " +
                "LEFT JOIN CommandeExterne ce ON f.id_fournisseur = ce.id_fournisseur " +
                "GROUP BY f.id_fournisseur, f.nom " +
                "ORDER BY montant_total DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("nom"),
                        rs.getInt("nb_articles"),
                        rs.getInt("nb_commandes"),
                        rs.getDouble("montant_total")
                });
            }
        }
    }

    private void genererStatsServices(Connection conn) throws SQLException {
        String[] columns = {"Service", "Nombre de commandes", "Articles demandés", "Responsable"};
        tableModel.setColumnIdentifiers(columns);

        String sql = "SELECT s.nom, COUNT(DISTINCT ci.id_commande) AS nb_commandes, " +
                "SUM(lci.quantite) AS total_articles, s.responsable " +
                "FROM Service s " +
                "LEFT JOIN Commande ci ON s.id_service = ci.id_service " +
                "LEFT JOIN LigneCommandeInterne lci ON ci.id_commande = lci.id_commande " +
                "GROUP BY s.id_service, s.nom, s.responsable " +
                "ORDER BY total_articles DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("nom"),
                        rs.getInt("nb_commandes"),
                        rs.getInt("total_articles"),
                        rs.getString("responsable")
                });
            }
        }
    }
}