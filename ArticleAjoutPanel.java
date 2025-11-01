import com.formdev.flatlaf.FlatLightLaf;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Date;

public class ArticleAjoutPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(33, 150, 243);
    private final Color HOVER_COLOR = new Color(30, 136, 229);
    private final Color BACKGROUND = new Color(245, 248, 250);
    private final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font INPUT_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private JTextField txtReference, txtNom, txtDescription, txtQuantite, txtPrix, txtSeuil;
    private JComboBox<String> comboType, comboCategorie, comboCodeLocal;
    private JDateChooser datePeremptionChooser;
    private final Runnable onBack;

    public ArticleAjoutPanel(Runnable onBack) {
        this.onBack = onBack;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 40, 20, 40));

        JLabel lblTitle = new JLabel(" Ajouter un Article");
        lblTitle.setFont(TITLE_FONT);
        lblTitle.setForeground(MAIN_COLOR);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblTitle, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        addFormRow(formPanel, "Référence", txtReference = createRoundedField());
        addFormRow(formPanel, "Nom", txtNom = createRoundedField());
        addFormRow(formPanel, "Description", txtDescription = createRoundedField());
        addFormRow(formPanel, "Quantité", txtQuantite = createRoundedField());
        JPanel datePanel = new JPanel(new BorderLayout(5, 5));
        datePanel.setOpaque(false);
        JLabel lblDate = new JLabel("Date de péremption");
        lblDate.setFont(LABEL_FONT);
        lblDate.setPreferredSize(new Dimension(150, 30));
        datePanel.add(lblDate, BorderLayout.WEST);
        datePeremptionChooser = new JDateChooser();
        datePeremptionChooser.setFont(INPUT_FONT);
        datePeremptionChooser.setPreferredSize(new Dimension(200, 30));
        datePanel.add(datePeremptionChooser, BorderLayout.CENTER);
        formPanel.add(datePanel);
        addFormRow(formPanel, "Prix unitaire", txtPrix = createRoundedField());
        addFormRow(formPanel, "Seuil d'alerte", txtSeuil = createRoundedField());
        addFormRow(formPanel, "Type", comboType = createCombo(new String[]{"Consommable", "Durable"}));
        addFormRow(formPanel, "Catégorie", comboCategorie = createCombo(new String[]{"Papeterie", "Informatique", "Électronique", "Mobilier", "Autre"}));
        addFormRow(formPanel, "Code local", comboCodeLocal = createCombo(new String[]{}));
        loadCodeLocaux();


        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        btnPanel.setOpaque(false);
        JButton btnAdd = createButton("Ajouter", MAIN_COLOR, e -> ajouterArticle());
        JButton btnBack = createButton("Retour", Color.GRAY, e -> onBack.run());
        btnPanel.add(btnAdd);
        btnPanel.add(btnBack);

        add(formPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        generateNewReference();
    }

    private void addFormRow(JPanel panel, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(5, 5));
        row.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(LABEL_FONT);
        lbl.setPreferredSize(new Dimension(150, 30));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setBorder(new EmptyBorder(8, 0, 8, 0));
        panel.add(row);
    }

    private JTextField createRoundedField() {
        JTextField field = new JTextField();
        field.setFont(INPUT_FONT);
        field.setPreferredSize(new Dimension(200, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MAIN_COLOR, 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        return field;
    }

    private JComboBox<String> createCombo(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(INPUT_FONT);
        combo.setPreferredSize(new Dimension(200, 30));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(MAIN_COLOR, 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        return combo;
    }

    private JButton createButton(String text, Color color, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(LABEL_FONT.deriveFont(Font.BOLD));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 30, 10, 30));
        btn.addActionListener(action);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(HOVER_COLOR);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }

    private void generateNewReference() {
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(reference) AS last_ref FROM Article")) {
            if (rs.next()) {
                String lastRef = rs.getString("last_ref");
                int num = (lastRef == null) ? 1 : Integer.parseInt(lastRef.replace("ART", "")) + 1;
                txtReference.setText(String.format("ART%03d", num));
            }
        } catch (Exception ex) {
            showError("Erreur lors de la génération de référence", ex.getMessage());
        }
    }

    private void loadCodeLocaux() {
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT code_local FROM LocalStockage")) {
            while (rs.next()) {
                comboCodeLocal.addItem(rs.getString("code_local"));
            }
        } catch (Exception e) {
            showError("Erreur de chargement des locaux", e.getMessage());
        }
    }

    private void ajouterArticle() {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Article (reference, nom, description, quantite, prix_unitaire, seuil_alerte, type, categorie, date_peremption, code_local) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

            if (!validateFields()) return;

            stmt.setString(1, txtReference.getText().trim());
            stmt.setString(2, txtNom.getText().trim());
            stmt.setString(3, txtDescription.getText().trim());
            stmt.setInt(4, Integer.parseInt(txtQuantite.getText().trim()));
            Date date = datePeremptionChooser.getDate();
            if (date != null)
                stmt.setDate(5, new java.sql.Date(date.getTime()));
            else
                stmt.setNull(5, Types.DATE);
            stmt.setDouble(6, Double.parseDouble(txtPrix.getText().trim()));
            stmt.setInt(7, Integer.parseInt(txtSeuil.getText().trim()));
            stmt.setString(8, (String) comboType.getSelectedItem());
            stmt.setString(9, (String) comboCategorie.getSelectedItem());


            stmt.setString(10, (String) comboCodeLocal.getSelectedItem());

            int res = stmt.executeUpdate();
            if (res > 0) {
                JOptionPane.showMessageDialog(this, "Article ajouté avec succès.", "Succès", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                generateNewReference();
            }
        } catch (SQLIntegrityConstraintViolationException ex) {
            showError("Erreur d'ajout", "La référence existe déjà. Veuillez réessayer.");
        } catch (Exception ex) {
            showError("Erreur d'ajout", ex.getMessage());
        }
    }

    private boolean validateFields() {
        try {
            if (txtReference.getText().isEmpty() || txtNom.getText().isEmpty() || comboCodeLocal.getSelectedItem() == null) {
                showError("Champs manquants", "Veuillez remplir tous les champs obligatoires");
                return false;
            }
            Integer.parseInt(txtQuantite.getText().trim());
            Double.parseDouble(txtPrix.getText().trim());
            Integer.parseInt(txtSeuil.getText().trim());
            return true;
        } catch (Exception e) {
            showError("Champs invalides", "Vérifiez les valeurs numériques.");
            return false;
        }
    }

    private void clearFields() {
        txtNom.setText("");
        txtDescription.setText("");
        txtQuantite.setText("");
        txtPrix.setText("");
        txtSeuil.setText("");
        comboType.setSelectedIndex(0);
        comboCategorie.setSelectedIndex(0);
        comboCodeLocal.setSelectedIndex(0);
        datePeremptionChooser.setDate(null);
    }

    private void showError(String title, String msg) {
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }
}
