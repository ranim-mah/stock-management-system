import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;

public class StockSortiePanel extends JPanel {
    private JComboBox<String> comboArticles;
    private JTextField txtQuantite, txtDestinataire;
    private JTextArea txtMotif;
    final Color MAIN_COLOR = new Color(12, 53, 106);
    final Color BG_COLOR = new Color(240, 240, 240, 200);

    public StockSortiePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        comboArticles = new JComboBox<>();
        chargerArticles();

        txtQuantite = new JTextField();
        txtDestinataire = new JTextField();
        txtMotif = new JTextArea();
        JScrollPane motifScroll = new JScrollPane(txtMotif);

        panel.add(new JLabel("Article:"));
        panel.add(comboArticles);
        panel.add(new JLabel("Quantité:"));
        panel.add(txtQuantite);
        panel.add(new JLabel("Destinataire:"));
        panel.add(txtDestinataire);
        panel.add(new JLabel("Motif:"));
        panel.add(motifScroll);

        JButton btnValider = new JButton("Valider la sortie");
        btnValider.addActionListener(e -> validerSortie());

        add(panel, BorderLayout.CENTER);
        add(btnValider, BorderLayout.SOUTH);
    }

    private void chargerArticles() {
        comboArticles.removeAllItems();
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT reference, nom FROM Article WHERE quantite > 0")) {

            while (rs.next()) {
                comboArticles.addItem(rs.getString("reference") + " - " + rs.getString("nom"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        return field;
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

    private void addStyledLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(MAIN_COLOR);
        panel.add(label);
    }

    private void validerSortie() {
        try {
            String article = (String) comboArticles.getSelectedItem();
            if (article == null || article.isEmpty()) {
                throw new IllegalArgumentException("Veuillez sélectionner un article");
            }

            String reference = article.split(" - ")[0];
            int quantite = Integer.parseInt(txtQuantite.getText().trim());
            String destinataire = txtDestinataire.getText().trim();
            String motif = txtMotif.getText().trim();

            if (quantite <= 0) {
                throw new IllegalArgumentException("La quantité doit être positive");
            }

            try (Connection conn = DatabaseConnector.getConnection()) {
                // Vérification stock
                try (PreparedStatement stmtCheck = conn.prepareStatement(
                        "SELECT quantite FROM Article WHERE reference = ?")) {
                    stmtCheck.setString(1, reference);
                    ResultSet rs = stmtCheck.executeQuery();

                    if (rs.next() && rs.getInt("quantite") < quantite) {
                        throw new IllegalArgumentException("Stock insuffisant");
                    }
                }

                // Mise à jour stock
                try (PreparedStatement stmtUpdate = conn.prepareStatement(
                        "UPDATE Article SET quantite = quantite - ? WHERE reference = ?")) {
                    stmtUpdate.setInt(1, quantite);
                    stmtUpdate.setString(2, reference);
                    stmtUpdate.executeUpdate();
                }

                // Enregistrement mouvement
                try (PreparedStatement stmtInsert = conn.prepareStatement(
                        "INSERT INTO MouvementStock (type_mouvement, reference_article, quantite, date, fournisseur, notes) " +
                                "VALUES ('SORTIE', ?, ?, ?, ?, ?)")) {
                    stmtInsert.setString(1, reference);
                    stmtInsert.setInt(2, quantite);
                    stmtInsert.setDate(3, Date.valueOf(LocalDate.now()));
                    stmtInsert.setString(4, destinataire);
                    stmtInsert.setString(5, motif);
                    stmtInsert.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Sortie de stock enregistrée avec succès !");
                reinitialiserFormulaire();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantité invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reinitialiserFormulaire() {
        txtQuantite.setText("");
        txtDestinataire.setText("");
        txtMotif.setText("");
        chargerArticles();
    }
}