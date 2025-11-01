import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class StockEntreePanel extends JPanel {

    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    private JComboBox<String> comboArticles;
    private JTextField txtQuantite, txtFournisseur;
    private JTextArea txtNotes;
    private Main main;

    public StockEntreePanel(Main mainFrame) {
        this.main = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setOpaque(false);
        initUI();
    }


    private void initUI() {
        JPanel glassPanel = new JPanel(new BorderLayout());
        glassPanel.setBackground(BG_COLOR);
        glassPanel.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 2),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 15, 15));
        formPanel.setOpaque(false);

        comboArticles = new JComboBox<>();
        chargerArticles();

        txtQuantite = createStyledTextField();
        txtFournisseur = createStyledTextField();
        txtNotes = new JTextArea();
        JScrollPane notesScroll = new JScrollPane(txtNotes);
        notesScroll.setBorder(new LineBorder(MAIN_COLOR, 1));

        addStyledLabel(formPanel, "Article:");
        formPanel.add(comboArticles);
        addStyledLabel(formPanel, "Quantité:");
        formPanel.add(txtQuantite);
        addStyledLabel(formPanel, "Fournisseur:");
        formPanel.add(txtFournisseur);
        addStyledLabel(formPanel, "Notes:");
        formPanel.add(notesScroll);

        JButton btnValider = createStyledButton("Enregistrer");
        btnValider.addActionListener(e -> enregistrerEntree());

        JButton btnRetour = createStyledButton("← Retour");
        btnRetour.addActionListener(e -> main.retourMenu());

        JPanel buttonPanel = new JPanel(new BorderLayout(10, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnRetour, BorderLayout.WEST);
        buttonPanel.add(btnValider, BorderLayout.EAST);

        glassPanel.add(formPanel, BorderLayout.CENTER);
        glassPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(glassPanel, BorderLayout.CENTER);
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

    private void chargerArticles() {
        comboArticles.removeAllItems();
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT reference, nom FROM Article")) {

            while (rs.next()) {
                comboArticles.addItem(rs.getString("reference") + " - " + rs.getString("nom"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enregistrerEntree() {
        try {
            String article = (String) comboArticles.getSelectedItem();
            String reference = article.split(" - ")[0];
            int quantite = Integer.parseInt(txtQuantite.getText().trim());
            String fournisseur = txtFournisseur.getText().trim();

            if (quantite <= 0) throw new IllegalArgumentException("La quantité doit être positive");

            try (Connection conn = DatabaseConnector.getConnection()) {
                // Récupérer le code_local de l'article
                String codeLocal;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT code_local FROM Article WHERE reference = ?")) {
                    stmt.setString(1, reference);
                    ResultSet rs = stmt.executeQuery();
                    rs.next();
                    codeLocal = rs.getString("code_local");
                }

                // Mise à jour du stock
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE Article SET quantite = quantite + ? WHERE reference = ?")) {
                    stmt.setInt(1, quantite);
                    stmt.setString(2, reference);
                    stmt.executeUpdate();
                }

                // Enregistrement du mouvement (ajusté au schéma de la table)
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO MouvementStock (type_mouvement, reference_article, quantite, code_local, date_mouvement) " +
                                "VALUES ('ENTREE', ?, ?, ?, ?)")) {
                    stmt.setString(1, reference);
                    stmt.setInt(2, quantite);
                    stmt.setString(3, codeLocal); // Code local récupéré
                    stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis())); // Date actuelle
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(main, "Entrée enregistrée !");
                main.retourMenu();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur SQL: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(main, e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}