import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class LocalGestionPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    private JTextField txtCode, txtNom, txtBatiment, txtNiveau, txtCapacite, txtResponsable;
    private JComboBox<String> comboType, comboLocaux;
    private JButton btnAjouter, btnModifier, btnSupprimer, btnVider;
    private DefaultTableModel tableModel;
    private JTable table;
    private Main main;
    
    public LocalGestionPanel(Main main) {
        final Color MAIN_COLOR = new Color(12, 53, 106);
        final Color BG_COLOR = new Color(240, 240, 240, 200);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setOpaque(false);
        initComponents();
        setupUI();
        chargerLocaux();
        chargerTableauLocaux();
    }


    private void initComponents() {
        comboLocaux = new JComboBox<>();
        txtCode = createStyledTextField(false);
        txtNom = createStyledTextField(false);
        comboType = new JComboBox<>(new String[]{"MAGASIN", "BUREAU", "SALLE", "AMPHI", "BIBLIOTHEQUE", "AUTRE"});
        txtBatiment = createStyledTextField(false);
        txtNiveau = createStyledTextField(false);
        txtCapacite = createStyledTextField(false);
        txtResponsable = createStyledTextField(false);

        btnAjouter = createStyledButton("Ajouter");
        btnModifier = createStyledButton("Modifier");
        btnSupprimer = createStyledButton("Supprimer");
        btnVider = createStyledButton("Vider");

        comboLocaux.addActionListener(e -> chargerDetailsLocal());
        btnAjouter.addActionListener(e -> ajouterLocal());
        btnModifier.addActionListener(e -> modifierLocal());
        btnSupprimer.addActionListener(e -> supprimerLocal());
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

        addStyledLabel(formPanel, "Local existant:");
        formPanel.add(comboLocaux);
        addStyledLabel(formPanel, "Code Local*:");
        formPanel.add(txtCode);
        addStyledLabel(formPanel, "Nom*:");
        formPanel.add(txtNom);
        addStyledLabel(formPanel, "Type*:");
        formPanel.add(comboType);
        addStyledLabel(formPanel, "Bâtiment:");
        formPanel.add(txtBatiment);
        addStyledLabel(formPanel, "Niveau:");
        formPanel.add(txtNiveau);
        addStyledLabel(formPanel, "Capacité Max:");
        formPanel.add(txtCapacite);
        addStyledLabel(formPanel, "Responsable:");
        formPanel.add(txtResponsable);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnAjouter);
        buttonPanel.add(btnModifier);
        buttonPanel.add(btnSupprimer);
        buttonPanel.add(btnVider);

        String[] columns = {"Code Local", "Nom", "Type", "Bâtiment", "Niveau", "Capacité", "Responsable"};
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

    private JTextField createStyledTextField(boolean disabled) {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        field.setEditable(!disabled);
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


    private void chargerLocaux() {
        comboLocaux.removeAllItems();
        comboLocaux.addItem("-- Nouveau local --");

        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT code_local, nom_local FROM LocalStockage")) {

            while (rs.next()) {
                comboLocaux.addItem(rs.getString("code_local") + " - " + rs.getString("nom_local"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chargerDetailsLocal() {
        if (comboLocaux.getSelectedIndex() <= 0) {
            viderFormulaire();
            return;
        }

        String[] parts = ((String) comboLocaux.getSelectedItem()).split(" - ");
        String codeLocal = parts[0];

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM LocalStockage WHERE code_local = ?")) {

            stmt.setString(1, codeLocal);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                txtCode.setText(rs.getString("code_local"));
                txtNom.setText(rs.getString("nom_local"));
                comboType.setSelectedItem(rs.getString("type_local"));
                txtBatiment.setText(rs.getString("batiment"));
                txtNiveau.setText(rs.getString("niveau"));
                txtCapacite.setText(String.valueOf(rs.getInt("capacite_max")));
                txtResponsable.setText(rs.getString("responsable"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chargerTableauLocaux() {
        tableModel.setRowCount(0);

        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM LocalStockage")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("code_local"),
                        rs.getString("nom_local"),
                        rs.getString("type_local"),
                        rs.getString("batiment"),
                        rs.getString("niveau"),
                        rs.getInt("capacite_max"),
                        rs.getString("responsable")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ajouterLocal() {
        if (!validerFormulaire()) return;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO LocalStockage (code_local, nom_local, type_local, batiment, niveau, capacite_max, responsable) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)")) {

            stmt.setString(1, txtCode.getText().trim());
            stmt.setString(2, txtNom.getText().trim());
            stmt.setString(3, (String) comboType.getSelectedItem());
            stmt.setString(4, txtBatiment.getText().trim());
            stmt.setString(5, txtNiveau.getText().trim());
            stmt.setInt(6, Integer.parseInt(txtCapacite.getText().trim()));
            stmt.setString(7, txtResponsable.getText().trim());

            stmt.executeUpdate();
            JOptionPane.showMessageDialog(main, "Local ajouté avec succès !");
            chargerLocaux();
            chargerTableauLocaux();
            viderFormulaire();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(main, "La capacité doit être un nombre valide",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur d'ajout: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modifierLocal() {
        if (comboLocaux.getSelectedIndex() <= 0 || !validerFormulaire()) return;

        String ancienCode = ((String) comboLocaux.getSelectedItem()).split(" - ")[0];
        String nouveauCode = txtCode.getText().trim();

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE LocalStockage SET code_local = ?, nom_local = ?, type_local = ?, " +
                             "batiment = ?, niveau = ?, capacite_max = ?, responsable = ? " +
                             "WHERE code_local = ?")) {

            stmt.setString(1, nouveauCode);
            stmt.setString(2, txtNom.getText().trim());
            stmt.setString(3, (String) comboType.getSelectedItem());
            stmt.setString(4, txtBatiment.getText().trim());
            stmt.setString(5, txtNiveau.getText().trim());
            stmt.setInt(6, Integer.parseInt(txtCapacite.getText().trim()));
            stmt.setString(7, txtResponsable.getText().trim());
            stmt.setString(8, ancienCode);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(main, "Local modifié avec succès !");
                chargerLocaux();
                chargerTableauLocaux();
                viderFormulaire();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(main, "La capacité doit être un nombre valide",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur de modification: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void supprimerLocal() {
        if (comboLocaux.getSelectedIndex() <= 0) return;

        String codeLocal = ((String) comboLocaux.getSelectedItem()).split(" - ")[0];

        int confirm = JOptionPane.showConfirmDialog(main,
                "Êtes-vous sûr de vouloir supprimer ce local?\nCette action est irréversible.",
                "Confirmation", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmtCheck = conn.prepareStatement(
                     "SELECT COUNT(*) FROM Article WHERE code_localisation = ?");
             PreparedStatement stmtDelete = conn.prepareStatement(
                     "DELETE FROM LocalStockage WHERE code_local = ?")) {

            // Vérifier les articles associés
            stmtCheck.setString(1, codeLocal);
            ResultSet rs = stmtCheck.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(main,
                        "Impossible de supprimer: des articles sont associés à ce local",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Suppression
            stmtDelete.setString(1, codeLocal);
            int rows = stmtDelete.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(main, "Local supprimé avec succès !");
                chargerLocaux();
                chargerTableauLocaux();
                viderFormulaire();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur de suppression: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viderFormulaire() {
        txtCode.setText("");
        txtNom.setText("");
        comboType.setSelectedIndex(0);
        txtBatiment.setText("");
        txtNiveau.setText("");
        txtCapacite.setText("");
        txtResponsable.setText("");
        comboLocaux.setSelectedIndex(0);
    }

    private boolean validerFormulaire() {
        if (txtCode.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(main, "Le code local est obligatoire",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (txtNom.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(main, "Le nom du local est obligatoire",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            Integer.parseInt(txtCapacite.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(main, "La capacité doit être un nombre valide",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
}