import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;

public class StockTransfertPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    private JComboBox<String> comboArticles, comboSource, comboDestination;
    private JTextField txtQuantite;
    private JButton btnValider;

    public StockTransfertPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel principal avec bordure
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MAIN_COLOR, 1),
                new EmptyBorder(10, 10, 10, 10)));
        mainPanel.setBackground(BG_COLOR);

        // Panel de formulaire
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setOpaque(false);

        comboArticles = createStyledComboBox();
        comboSource = createStyledComboBox();
        comboDestination = createStyledComboBox();
        txtQuantite = createStyledTextField();


        addFormRow(formPanel, "Article:", comboArticles);
        addFormRow(formPanel, "Local source:", comboSource);
        addFormRow(formPanel, "Local destination:", comboDestination);
        addFormRow(formPanel, "Quantité:", txtQuantite);

        // Panel de bouton
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        btnValider = createStyledButton("Valider le transfert");
        btnValider.addActionListener(e -> validerTransfert());
        buttonPanel.add(btnValider);

        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);

        chargerArticles();
        chargerLocaux();
    }

    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Arial", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MAIN_COLOR, 1),
                new EmptyBorder(5, 10, 5, 10)));
        return combo;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MAIN_COLOR, 1),
                new EmptyBorder(5, 10, 5, 10)));
        return field;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(MAIN_COLOR);
        btn.setBorder(new EmptyBorder(10, 25, 10, 25));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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

    private void addFormRow(JPanel panel, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        lbl.setForeground(MAIN_COLOR);
        panel.add(lbl);
        panel.add(field);
    }

    private void chargerArticles() {
        SwingUtilities.invokeLater(() -> {
            comboArticles.removeAllItems();
            try (Connection conn = DatabaseConnector.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT reference, nom FROM Article WHERE quantite > 0")) {

                while (rs.next()) {
                    comboArticles.addItem(rs.getString("reference") + " - " + rs.getString("nom"));
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erreur de chargement des articles: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void chargerLocaux() {
        SwingUtilities.invokeLater(() -> {
            comboSource.removeAllItems();
            comboDestination.removeAllItems();

            try (Connection conn = DatabaseConnector.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT code_local, nom_local FROM LocalStockage")) {

                while (rs.next()) {
                    String item = rs.getString("code_local") + " - " + rs.getString("nom_local");
                    comboSource.addItem(item);
                    comboDestination.addItem(item);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erreur de chargement des locaux: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void validerTransfert() {
        String articleSelection = (String) comboArticles.getSelectedItem();
        String sourceSelection = (String) comboSource.getSelectedItem();
        String destSelection = (String) comboDestination.getSelectedItem();
        String quantiteText = txtQuantite.getText().trim();

        // Validation des entrées
        if (articleSelection == null || sourceSelection == null || destSelection == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner tous les champs", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (sourceSelection.equals(destSelection)) {
            JOptionPane.showMessageDialog(this, "Les locaux source et destination doivent être différents", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int quantite;
        try {
            quantite = Integer.parseInt(quantiteText);
            if (quantite <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer une quantité valide (nombre positif)", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String reference = articleSelection.split(" - ")[0];
        String codeSource = sourceSelection.split(" - ")[0];
        String codeDest = destSelection.split(" - ")[0];

        // Exécution du transfert dans un thread séparé
        new Thread(() -> {
            try (Connection conn = DatabaseConnector.getConnection()) {
                conn.setAutoCommit(false);

                // 1. Vérifier le stock disponible
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT quantite FROM Article WHERE reference = ? AND code_local = ? FOR UPDATE")) {
                    stmt.setString(1, reference);
                    stmt.setString(2, codeSource);
                    ResultSet rs = stmt.executeQuery();

                    if (!rs.next()) {
                        throw new SQLException("Article non trouvé dans le local source");
                    }

                    int stockDisponible = rs.getInt("quantite");
                    if (stockDisponible < quantite) {
                        throw new SQLException("Stock insuffisant dans le local source");
                    }
                }

                // 2. Mettre à jour le stock source
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE Article SET quantite = quantite - ? WHERE reference = ? AND code_local = ?")) {
                    stmt.setInt(1, quantite);
                    stmt.setString(2, reference);
                    stmt.setString(3, codeSource);
                    stmt.executeUpdate();
                }

                // 3. Vérifier si l'article existe déjà dans la destination
                boolean articleExiste = false;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT 1 FROM Article WHERE reference = ? AND code_local = ?")) {
                    stmt.setString(1, reference);
                    stmt.setString(2, codeDest);
                    articleExiste = stmt.executeQuery().next();
                }

                // 4. Mettre à jour ou insérer dans la destination
                if (articleExiste) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE Article SET quantite = quantite + ? WHERE reference = ? AND code_local = ?")) {
                        stmt.setInt(1, quantite);
                        stmt.setString(2, reference);
                        stmt.setString(3, codeDest);
                        stmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO Article (reference, nom, description, type, categorie, quantite, " +
                                    "prix_unitaire, seuil_alerte, code_local, date_peremption, critique) " +
                                    "SELECT reference, nom, description, type, categorie, ?, " +
                                    "prix_unitaire, seuil_alerte, ?, date_peremption, critique FROM Article " +
                                    "WHERE reference = ? AND code_local = ?")) {
                        stmt.setInt(1, quantite);
                        stmt.setString(2, codeDest);
                        stmt.setString(3, reference);
                        stmt.setString(4, codeSource);
                        stmt.executeUpdate();
                    }
                }

                // 5. Enregistrer les mouvements de stock
                double prixUnitaire = 0;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT prix_unitaire FROM Article WHERE reference = ? AND code_local = ?")) {
                    stmt.setString(1, reference);
                    stmt.setString(2, codeSource);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        prixUnitaire = rs.getDouble("prix_unitaire");
                    }
                }

                // Sortie du stock source
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO MouvementStock (type_mouvement, reference_article, quantite, " +
                                "prix_unitaire, code_local, date_mouvement) " +
                                "VALUES ('SORTIE', ?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, reference);
                    stmt.setInt(2, quantite);
                    stmt.setDouble(3, prixUnitaire);
                    stmt.setString(4, codeSource);
                    stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.executeUpdate();
                }

                // Entrée dans le stock destination
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO MouvementStock (type_mouvement, reference_article, quantite, " +
                                "prix_unitaire, code_local, date_mouvement) " +
                                "VALUES ('ENTREE', ?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, reference);
                    stmt.setInt(2, quantite);
                    stmt.setDouble(3, prixUnitaire);
                    stmt.setString(4, codeDest);
                    stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.executeUpdate();
                }

                conn.commit();

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Transfert effectué avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                    txtQuantite.setText("");
                    chargerArticles();
                });

            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Erreur lors du transfert: " + e.getMessage(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                });
                try {
                    if (DatabaseConnector.getConnection() != null) {
                        DatabaseConnector.getConnection().rollback();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }
}