import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ArticleSupprimerPanel extends JPanel {
    private final Main main;
    private final JComboBox<String> comboArticles;
    private final JEditorPane detailsPane;
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BACKGROUND_COLOR = new Color(240, 240, 240, 200);
    private final Font TITLE_FONT = new Font("Arial", Font.BOLD, 24);
    private final Font TEXT_FONT = new Font("Arial", Font.PLAIN, 14);

    public ArticleSupprimerPanel(Main main) {
        this.main = main;
        setLayout(new BorderLayout(20, 20));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Panel principal avec effet de verre
        JPanel glassPanel = new JPanel(new BorderLayout(15, 15));
        glassPanel.setBackground(new Color(255, 255, 255, 180));
        glassPanel.setBorder(new CompoundBorder(
                new MatteBorder(2, 2, 2, 2, MAIN_COLOR),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // Header stylisé
        JLabel lblTitle = new JLabel("🗑️ SUPPRESSION D'ARTICLE");
        lblTitle.setFont(TITLE_FONT);
        lblTitle.setForeground(MAIN_COLOR);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setBorder(new EmptyBorder(0, 0, 20, 0));

        // ComboBox amélioré avec placeholder
        comboArticles = new JComboBox<>();
        comboArticles.setRenderer(new ModernComboBoxRenderer());
        comboArticles.setFont(TEXT_FONT.deriveFont(16f));
        comboArticles.setBackground(Color.WHITE);
        comboArticles.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 1),
                new EmptyBorder(10, 15, 10, 15)
        ));
        comboArticles.addActionListener(e -> updateDetails());

        // Zone de détails en HTML
        detailsPane = new JEditorPane();
        detailsPane.setContentType("text/html");
        detailsPane.setEditable(false);
        detailsPane.setBackground(new Color(255, 255, 255, 200));
        detailsPane.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(MAIN_COLOR, 1), "📋 Fiche technique",
                        TitledBorder.LEFT, TitledBorder.TOP, TITLE_FONT, MAIN_COLOR),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Boutons premium
        JButton btnSupprimer = createHoverButton("SUPPRIMER", new Color(200, 50, 50));
        JButton btnRetour = createHoverButton("RETOUR AU MENU", MAIN_COLOR);

        // Configuration des actions
        btnSupprimer.addActionListener(e -> performDeletion());
        btnRetour.addActionListener(e -> main.retourMenu());

        // Assemblage
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);
        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(comboArticles, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(btnRetour);
        btnPanel.add(btnSupprimer);

        glassPanel.add(headerPanel, BorderLayout.NORTH);
        glassPanel.add(new JScrollPane(detailsPane), BorderLayout.CENTER);
        glassPanel.add(btnPanel, BorderLayout.SOUTH);

        add(glassPanel, BorderLayout.CENTER);
        loadArticles();
    }


    private JButton createHoverButton(String text, Color baseColor) {
        JButton btn = new JButton(text);
        btn.setFont(TEXT_FONT.deriveFont(Font.BOLD, 16f));
        btn.setForeground(Color.WHITE);
        btn.setBackground(baseColor);
        btn.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 1),
                new EmptyBorder(12, 30, 12, 30)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(baseColor.darker());
                btn.setBorder(new CompoundBorder(
                        new LineBorder(MAIN_COLOR.darker(), 2),
                        new EmptyBorder(12, 30, 12, 30)
                ));
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(baseColor);
                btn.setBorder(new CompoundBorder(
                        new LineBorder(MAIN_COLOR, 1),
                        new EmptyBorder(12, 30, 12, 30)
                ));
            }
        });

        return btn;
    }

    private void loadArticles() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DatabaseConnector.getConnection();
                     Statement stmt = conn.createStatement()) {

                    ResultSet rs = stmt.executeQuery("SELECT reference, nom FROM Article");
                    comboArticles.removeAllItems();
                    while (rs.next()) {
                        comboArticles.addItem(rs.getString("reference") + " - " + rs.getString("nom"));
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                if (comboArticles.getItemCount() > 0) {
                    comboArticles.setSelectedIndex(0);
                }
            }
        };
        worker.execute();
    }

    private void updateDetails() {
        String selected = (String) comboArticles.getSelectedItem();
        if (selected == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            String htmlContent = "";

            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DatabaseConnector.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT a.*, l.nom_local FROM Article a " +
                                     "JOIN LocalStockage l ON a.code_local = l.code_local " +
                                     "WHERE a.reference = ?")) {

                    stmt.setString(1, selected.split(" - ")[0]);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        htmlContent = "<html><style>"
                                + "body { font-family: 'Arial'; font-size: 14px; color: #333; }"
                                + ".detail { margin: 12px 0; padding: 8px; background: rgba(255,255,255,0.9); border-radius: 5px; }"
                                + "b { color: #0c356a; }"
                                + "</style>"
                                + "<div class='detail'><b>🔖 Référence:</b> " + rs.getString("reference") + "</div>"
                                + "<div class='detail'><b>📛 Nom:</b> " + rs.getString("nom") + "</div>"
                                + "<div class='detail'><b>📦 Type:</b> " + rs.getString("type") + "</div>"
                                + "<div class='detail'><b>🏷️ Catégorie:</b> " + rs.getString("categorie") + "</div>"
                                + "<div class='detail'><b>🧮 Quantité:</b> " + rs.getInt("quantite") + "</div>"
                                + "<div class='detail'><b>💰 Prix unitaire:</b> " + String.format("%.2f DT", rs.getDouble("prix_unitaire")) + "</div>"
                                + "<div class='detail'><b>🏢 Localisation:</b> " + rs.getString("code_local") + " (" + rs.getString("nom_local") + ")</div>"
                                + "</html>";
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                detailsPane.setText(htmlContent);
            }
        };
        worker.execute();
    }

    private void performDeletion() {
        String selected = (String) comboArticles.getSelectedItem();
        if (selected == null) return;

        Object[] options = {"Confirmer", "Annuler"};
        int choice = JOptionPane.showOptionDialog(main,
                "<html><div style='width: 300px; padding: 15px;'>"
                        + "<h3 style='color: #d32f2f; margin-top: 0;'>⚠️ Attention !</h3>"
                        + "<p>Cette action supprimera définitivement :</p>"
                        + "<p style='font-weight: bold; margin: 10px 0;'>" + selected + "</p>"
                        + "<p>Êtes-vous absolument certain ?</p></div></html>",
                "Confirmation finale",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);

        if (choice == 0) {
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    try (Connection conn = DatabaseConnector.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(
                                 "DELETE FROM Article WHERE reference = ?")) {

                        stmt.setString(1, selected.split(" - ")[0]);
                        return stmt.executeUpdate() > 0;
                    }
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(main,
                                    "<html><div style='text-align: center; padding: 20px;'>"
                                            + "<h3 style='color: #0c356a;'>✅ Suppression réussie !</h3>"
                                            + "<p>L'article a été définitivement supprimé</p></div></html>");
                            loadArticles();
                            detailsPane.setText("");
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(main,
                                "<html><div style='color: #d32f2f; padding: 15px;'>"
                                        + "Erreur : " + e.getMessage() + "</div></html>",
                                "Échec de suppression",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    // Renderer personnalisé pour le JComboBox
    private static class ModernComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBorder(new EmptyBorder(8, 15, 8, 15));
            setFont(getFont().deriveFont(16f));
            return this;
        }
    }
}