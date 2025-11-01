import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class FournisseurGestionPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    private JTextField txtId, txtNom, txtAdresse, txtContact;
    private JComboBox<String> comboFournisseurs, comboReputation;
    private JButton btnAjouter, btnModifier, btnSupprimer, btnVider;
    private DefaultTableModel tableModel;
    private JTable table;
    private Main main;

    public FournisseurGestionPanel(Main main) {
        this.main = main;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setOpaque(false);
        initComponents();
        setupUI();
        loadDataFromDB();
    }

    private void initComponents() {
        comboFournisseurs = new JComboBox<>();
        comboReputation = new JComboBox<>(new String[]{"excellent", "bon", "moyen", "mauvais"});
        txtId = createStyledTextField(false);
        txtNom = createStyledTextField(false);
        txtAdresse = createStyledTextField(false);
        txtContact = createStyledTextField(false);

        btnAjouter = createStyledButton("Ajouter");
        btnModifier = createStyledButton("Modifier");
        btnSupprimer = createStyledButton("Supprimer");
        btnVider = createStyledButton("Vider");

        comboFournisseurs.addActionListener(e -> chargerDetailsFournisseur());
        btnAjouter.addActionListener(e -> ajouterFournisseur());
        btnModifier.addActionListener(e -> modifierFournisseur());
        btnSupprimer.addActionListener(e -> supprimerFournisseur());
        btnVider.addActionListener(e -> viderFormulaire());
    }

    private void setupUI() {
        JPanel glassPanel = new JPanel(new BorderLayout(10, 10));
        glassPanel.setBackground(BG_COLOR);
        glassPanel.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 2),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 15, 15));
        formPanel.setOpaque(false);

        addStyledLabel(formPanel, "Fournisseur existant:");
        formPanel.add(comboFournisseurs);
        addStyledLabel(formPanel, "ID:");
        formPanel.add(txtId);
        addStyledLabel(formPanel, "Nom*:");
        formPanel.add(txtNom);
        addStyledLabel(formPanel, "Adresse:");
        formPanel.add(txtAdresse);
        addStyledLabel(formPanel, "Contact:");
        formPanel.add(txtContact);
        addStyledLabel(formPanel, "Réputation:");
        formPanel.add(comboReputation);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnAjouter);
        buttonPanel.add(btnModifier);
        buttonPanel.add(btnSupprimer);
        buttonPanel.add(btnVider);

        String[] columns = {"ID", "Nom", "Adresse", "Contact", "Réputation"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.setGridColor(MAIN_COLOR);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(MAIN_COLOR, 1));

        glassPanel.add(formPanel, BorderLayout.NORTH);
        glassPanel.add(scrollPane, BorderLayout.CENTER);
        glassPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(glassPanel, BorderLayout.CENTER);
    }

    private JTextField createStyledTextField(boolean b) {
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

    private void loadDataFromDB() {
        chargerFournisseurs();
        chargerTableauFournisseurs();
    }

    // Dans FournisseurGestionPanel.java, méthode chargerFournisseurs()
    private void chargerFournisseurs() {
        comboFournisseurs.removeAllItems();
        comboFournisseurs.addItem("-- Nouveau fournisseur --"); // Ajout de l'élément par défaut

        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_fournisseur, nom FROM Fournisseur ORDER BY nom")) {

            // Traitement immédiat du ResultSet
            while (rs.next()) {
                comboFournisseurs.addItem(rs.getString("id_fournisseur") + " - " + rs.getString("nom"));
            }

            comboFournisseurs.revalidate();
            comboFournisseurs.repaint();

        } catch (SQLException e) {
            showError("Erreur de chargement des fournisseurs : " + e.getMessage());
        }
    }

    private void chargerDetailsFournisseur() {
        if (comboFournisseurs.getSelectedIndex() <= 0) {
            viderFormulaire();
            return;
        }

        String selected = (String) comboFournisseurs.getSelectedItem();
        String idFournisseur = selected.split(" - ")[0];

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Fournisseur WHERE id_fournisseur = ?")) {

            stmt.setString(1, idFournisseur);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                txtId.setText(rs.getString("id_fournisseur"));
                txtNom.setText(rs.getString("nom"));
                txtAdresse.setText(rs.getString("adresse"));
                txtContact.setText(rs.getString("contact"));
                comboReputation.setSelectedItem(rs.getString("reputation"));
            }
        } catch (SQLException e) {
            showError("Erreur de chargement des détails : " + e.getMessage());
        }
    }

    private void chargerTableauFournisseurs() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);

            try (Connection conn = DatabaseConnector.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM Fournisseur ORDER BY nom")) {

                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getString("id_fournisseur"),
                            rs.getString("nom"),
                            rs.getString("adresse"),
                            rs.getString("contact"),
                            rs.getString("reputation")
                    });
                }
                tableModel.fireTableDataChanged();
            } catch (SQLException e) {
                showError("Erreur de chargement du tableau : " + e.getMessage());
            }
        });
    }

    private void ajouterFournisseur() {
        if (!validerFormulaire()) return;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Fournisseur (id_fournisseur, nom, adresse, contact, reputation) VALUES (?, ?, ?, ?, ?)")) {

            stmt.setString(1, txtId.getText().trim());
            stmt.setString(2, txtNom.getText().trim());
            stmt.setString(3, txtAdresse.getText().trim());
            stmt.setString(4, txtContact.getText().trim());
            stmt.setString(5, comboReputation.getSelectedItem().toString());

            if (stmt.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(main, "Fournisseur ajouté avec succès !");
                refreshInterface();
            }
        } catch (SQLException e) {
            showError("Erreur d'ajout : " + e.getMessage());
        }
    }

    private void modifierFournisseur() {
        if (comboFournisseurs.getSelectedIndex() <= 0 || !validerFormulaire()) return;

        String selected = (String) comboFournisseurs.getSelectedItem();
        String idFournisseur = selected.split(" - ")[0];

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Fournisseur SET nom = ?, adresse = ?, contact = ?, reputation = ? WHERE id_fournisseur = ?")) {

            stmt.setString(1, txtNom.getText().trim());
            stmt.setString(2, txtAdresse.getText().trim());
            stmt.setString(3, txtContact.getText().trim());
            stmt.setString(4, comboReputation.getSelectedItem().toString());
            stmt.setString(5, idFournisseur);

            if (stmt.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(main, "Fournisseur modifié avec succès !");
                refreshInterface();
            }
        } catch (SQLException e) {
            showError("Erreur de modification : " + e.getMessage());
        }
    }

    private void supprimerFournisseur() {
        if (comboFournisseurs.getSelectedIndex() <= 0) return;

        String selected = (String) comboFournisseurs.getSelectedItem();
        String idFournisseur = selected.split(" - ")[0];

        int confirm = JOptionPane.showConfirmDialog(main,
                "Êtes-vous sûr de vouloir supprimer ce fournisseur ?\nCette action est irréversible.",
                "Confirmation", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmtCheck = conn.prepareStatement("SELECT COUNT(*) FROM Article WHERE id_fournisseur = ?");
             PreparedStatement stmtDelete = conn.prepareStatement("DELETE FROM Fournisseur WHERE id_fournisseur = ?")) {

            stmtCheck.setString(1, idFournisseur);
            ResultSet rs = stmtCheck.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(main, "Impossible de supprimer : articles associés existants", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            stmtDelete.setString(1, idFournisseur);
            if (stmtDelete.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(main, "Fournisseur supprimé avec succès !");
                refreshInterface();
            }
        } catch (SQLException e) {
            showError("Erreur de suppression : " + e.getMessage());
        }
    }

    private void refreshInterface() {
        viderFormulaire();
        chargerFournisseurs();
        chargerTableauFournisseurs();
    }

    private void viderFormulaire() {
        txtId.setText("");
        txtNom.setText("");
        txtAdresse.setText("");
        txtContact.setText("");
        comboReputation.setSelectedIndex(0);
        comboFournisseurs.setSelectedIndex(0);
    }

    private boolean validerFormulaire() {
        if (txtId.getText().trim().isEmpty()) {
            showError("L'ID est obligatoire");
            return false;
        }
        if (txtNom.getText().trim().isEmpty()) {
            showError("Le nom est obligatoire");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(main, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}