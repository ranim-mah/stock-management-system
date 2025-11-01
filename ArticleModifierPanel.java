import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Date;

import com.toedter.calendar.JDateChooser;

public class ArticleModifierPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color SECONDARY_COLOR = new Color(240, 240, 240);
    private final Font TITLE_FONT = new Font("Arial", Font.BOLD, 24);
    private final Font LABEL_FONT = new Font("Arial", Font.BOLD, 14);
    private final Font INPUT_FONT = new Font("Arial", Font.PLAIN, 14);

    private JComboBox<String> comboArticles;
    private JTextField txtNom, txtDescription, txtQuantite, txtPrix, txtSeuil;
    private JComboBox<String> comboType, comboCategorie;
    private JDateChooser datePeremptionChooser;
    private Main main;

    public ArticleModifierPanel(Main main) {
        this.main = main;
        setLayout(new BorderLayout(20, 20));
        setBackground(SECONDARY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Header Panel avec calendrier en haut
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);

        // Calendrier en haut
        datePeremptionChooser = new JDateChooser();
        datePeremptionChooser.setDateFormatString("yyyy-MM-dd");
        datePeremptionChooser.setFont(INPUT_FONT);
        JPanel calendarPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        calendarPanel.add(datePeremptionChooser);
        headerPanel.add(calendarPanel, BorderLayout.NORTH);

        JLabel lblTitle = new JLabel("MODIFICATION D'ARTICLE");
        lblTitle.setFont(TITLE_FONT);
        lblTitle.setForeground(MAIN_COLOR);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(lblTitle, BorderLayout.CENTER);

        // Form Panel
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 15, 15));
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // Initialize components
        comboArticles = createStyledComboBox();
        txtNom = createStyledTextField();
        txtDescription = createStyledTextField();
        txtQuantite = createStyledTextField();
        txtPrix = createStyledTextField();
        txtSeuil = createStyledTextField();
        comboType = createStyledComboBox(new String[]{"Consommable", "Durable"});
        comboCategorie = createStyledComboBox(new String[]{"Papeterie", "Informatique", "Électronique", "Mobilier", "Autre"});

        // Add components to form
        addFormRow(formPanel, "Sélectionner l'article :", comboArticles);
        formPanel.add(new JLabel());
        formPanel.add(createActionButton("CHARGER", MAIN_COLOR, e -> chargerDonneesArticle()));
        addFormRow(formPanel, "Nom :", txtNom);
        addFormRow(formPanel, "Description :", txtDescription);
        addFormRow(formPanel, "Quantité :", txtQuantite);
        addFormRow(formPanel, "Prix unitaire :", txtPrix);
        addFormRow(formPanel, "Seuil d'alerte :", txtSeuil);
        addFormRow(formPanel, "Type :", comboType);
        addFormRow(formPanel, "Catégorie :", comboCategorie);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(createActionButton("ENREGISTRER", MAIN_COLOR, e -> validerModifications()));
        buttonPanel.add(createActionButton("RETOUR", new Color(100, 100, 100), e -> main.retourMenu()));

        // Assembly
        add(headerPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        chargerArticles();
    }

    // Méthodes manquantes ajoutées
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(INPUT_FONT);
        field.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 2, 0, MAIN_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));
        return field;
    }

    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(INPUT_FONT);
        combo.setBackground(Color.WHITE);
        combo.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 2, 0, MAIN_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));
        return combo;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(INPUT_FONT);
        combo.setBackground(Color.WHITE);
        combo.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 2, 0, MAIN_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));
        return combo;
    }

    private void addFormRow(JPanel panel, String labelText, JComponent component) {
        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(MAIN_COLOR);
        panel.add(label);
        panel.add(component);
    }

    private JButton createActionButton(String text, Color bgColor, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(LABEL_FONT);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setBorder(new EmptyBorder(10, 30, 10, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgColor.darker());
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });
        return btn;
    }

    private void chargerArticles() {
        comboArticles.removeAllItems();
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT reference, nom FROM Article ORDER BY nom")) {

            while (rs.next()) {
                comboArticles.addItem(rs.getString("reference") + " - " + rs.getString("nom"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de chargement: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chargerDonneesArticle() {
        String selected = (String) comboArticles.getSelectedItem();
        if (selected == null || selected.isEmpty()) return;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Article WHERE reference = ?")) {

            stmt.setString(1, selected.split(" - ")[0]);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                txtNom.setText(rs.getString("nom"));
                txtDescription.setText(rs.getString("description"));
                txtQuantite.setText(String.valueOf(rs.getInt("quantite")));
                txtPrix.setText(String.format("%.2f", rs.getDouble("prix_unitaire")));
                txtSeuil.setText(String.valueOf(rs.getInt("seuil_alerte")));
                comboType.setSelectedItem(rs.getString("type"));
                comboCategorie.setSelectedItem(rs.getString("categorie"));

                java.sql.Date peremption = rs.getDate("date_peremption");
                datePeremptionChooser.setDate(peremption != null ? new Date(peremption.getTime()) : null);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de chargement: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void validerModifications() {
        String selected = (String) comboArticles.getSelectedItem();
        if (selected == null || selected.isEmpty()) return;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Article SET nom = ?, description = ?, quantite = ?, prix_unitaire = ?, seuil_alerte = ?, type = ?, categorie = ?, date_peremption = ? WHERE reference = ?")) {

            if (!validateInputs()) return;

            stmt.setString(1, txtNom.getText().trim());
            stmt.setString(2, txtDescription.getText().trim());
            stmt.setInt(3, Integer.parseInt(txtQuantite.getText().trim()));
            stmt.setDouble(4, Double.parseDouble(txtPrix.getText().trim()));
            stmt.setInt(5, Integer.parseInt(txtSeuil.getText().trim()));
            stmt.setString(6, (String) comboType.getSelectedItem());
            stmt.setString(7, (String) comboCategorie.getSelectedItem());

            Date selectedDate = datePeremptionChooser.getDate();
            if (selectedDate != null) {
                stmt.setDate(8, new java.sql.Date(selectedDate.getTime()));
            } else {
                stmt.setNull(8, Types.DATE);
            }

            stmt.setString(9, selected.split(" - ")[0]);

            if (stmt.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(this, "Article modifié avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                main.retourMenu();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de modification: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateInputs() {
        try {
            Integer.parseInt(txtQuantite.getText().trim());
            Double.parseDouble(txtPrix.getText().trim());
            Integer.parseInt(txtSeuil.getText().trim());
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer des valeurs numériques valides!", "Erreur de format", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}