import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ServiceGestionPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);
    private final Font TITLE_FONT = new Font("Arial", Font.BOLD, 20);

    private JTextField txtId, txtNom, txtResponsable, txtLocalisation, txtTelephone;
    private JComboBox<String> comboServices;
    private JButton btnAjouter, btnModifier, btnSupprimer, btnVider;
    private DefaultTableModel tableModel;
    private JTable table;
    private Main main;

    public ServiceGestionPanel(Main main) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setOpaque(false);

        // Panel principal avec effet de verre
        JPanel glassPanel = new JPanel(new BorderLayout(10, 10));
        glassPanel.setBackground(BG_COLOR);
        glassPanel.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 2),
                new EmptyBorder(15, 15, 15, 15)
        ));

        initComponents();
        setupLayout(glassPanel);
        glassPanel.add(createTablePanel(), BorderLayout.CENTER);

        add(glassPanel, BorderLayout.CENTER);
        chargerDonnees();
    }


    private void initComponents() {
        comboServices = new JComboBox<>();
        txtId = createStyledTextField(true);
        txtNom = createStyledTextField(false);
        txtResponsable = createStyledTextField(false);
        txtLocalisation = createStyledTextField(false);
        txtTelephone = createStyledTextField(false);

        btnAjouter = createStyledButton("Ajouter");
        btnModifier = createStyledButton("Modifier");
        btnSupprimer = createStyledButton("Supprimer");
        btnVider = createStyledButton("Vider");

        btnAjouter.addActionListener(e -> ajouterService());
        btnModifier.addActionListener(e -> modifierService());
        btnSupprimer.addActionListener(e -> supprimerService());
        btnVider.addActionListener(e -> viderFormulaire());
        comboServices.addActionListener(e -> chargerDetailsService());
    }

    private void setupLayout(JPanel parent) {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setOpaque(false);

        addStyledLabel(formPanel, "Service existant:");
        formPanel.add(comboServices);
        addStyledLabel(formPanel, "ID:");
        formPanel.add(txtId);
        addStyledLabel(formPanel, "Nom*:");
        formPanel.add(txtNom);
        addStyledLabel(formPanel, "Responsable:");
        formPanel.add(txtResponsable);
        addStyledLabel(formPanel, "Localisation:");
        formPanel.add(txtLocalisation);
        addStyledLabel(formPanel, "Téléphone:");
        formPanel.add(txtTelephone);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnAjouter);
        buttonPanel.add(btnModifier);
        buttonPanel.add(btnSupprimer);
        buttonPanel.add(btnVider);

        parent.add(formPanel, BorderLayout.NORTH);
        parent.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createTablePanel() {
        String[] columns = {"ID", "Nom", "Responsable", "Localisation", "Téléphone"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.setGridColor(MAIN_COLOR);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(MAIN_COLOR, 1));
        return new JPanel(new BorderLayout()) {{
            add(scrollPane);
        }};
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

    private void chargerDonnees() {
        chargerServices();
        chargerTableauServices();
    }

    private void chargerServices() {
        comboServices.removeAllItems();
        comboServices.addItem("-- Nouveau service --");

        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id_service, nom FROM Service ORDER BY nom")) {

            while (rs.next()) {
                comboServices.addItem(rs.getInt("id_service") + " - " + rs.getString("nom"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de chargement des services: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chargerDetailsService() {
        if (comboServices.getSelectedIndex() <= 0) {
            viderFormulaire();
            return;
        }

        String selected = (String) comboServices.getSelectedItem();
        int idService = Integer.parseInt(selected.split(" - ")[0]);

        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM Service WHERE id_service = ?")) {

            stmt.setInt(1, idService);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                txtId.setText(String.valueOf(rs.getInt("id_service")));
                txtNom.setText(rs.getString("nom"));
                txtResponsable.setText(rs.getString("responsable"));
                txtLocalisation.setText(rs.getString("localisation"));
                txtTelephone.setText(rs.getString("telephone"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chargerTableauServices() {
        tableModel.setRowCount(0);

        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM Service ORDER BY nom")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id_service"),
                        rs.getString("nom"),
                        rs.getString("responsable"),
                        rs.getString("localisation"),
                        rs.getString("telephone")
                });
            }
            tableModel.fireTableDataChanged();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ajouterService() {
        if (!validerFormulaire()) {
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Service (nom, responsable, localisation, telephone) "
                        + "VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, txtNom.getText().trim());
            stmt.setString(2, txtResponsable.getText().trim());
            stmt.setString(3, txtLocalisation.getText().trim());
            stmt.setString(4, txtTelephone.getText().trim());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        txtId.setText(String.valueOf(rs.getInt(1)));
                    }
                }

                JOptionPane.showMessageDialog(this, "Service ajouté avec succès!");
                chargerServices();
                chargerTableauServices();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur d'ajout: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modifierService() {
        if (comboServices.getSelectedIndex() <= 0 || !validerFormulaire()) {
            return;
        }

        String selected = (String) comboServices.getSelectedItem();
        int idService = Integer.parseInt(selected.split(" - ")[0]);

        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "UPDATE Service SET nom = ?, responsable = ?, localisation = ?, telephone = ? "
                        + "WHERE id_service = ?")) {

            stmt.setString(1, txtNom.getText().trim());
            stmt.setString(2, txtResponsable.getText().trim());
            stmt.setString(3, txtLocalisation.getText().trim());
            stmt.setString(4, txtTelephone.getText().trim());
            stmt.setInt(5, idService);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Service modifié avec succès!");
                chargerServices();
                chargerTableauServices();
                viderFormulaire(); // <-- Ajoutez cette ligne
                comboServices.setSelectedIndex(0);
                comboServices.revalidate(); // <-- Ajoutez cette ligne
                comboServices.repaint();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de modification: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void supprimerService() {
        if (comboServices.getSelectedIndex() <= 0) {
            return;
        }

        String selected = (String) comboServices.getSelectedItem();
        int idService = Integer.parseInt(selected.split(" - ")[0]);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Êtes-vous sûr de vouloir supprimer ce service?\nCette action est irréversible.",
                "Confirmation", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement stmtCheck = conn.prepareStatement(
                "SELECT COUNT(*) FROM LigneCommandeInterne WHERE id_service = ?"); PreparedStatement stmtDelete = conn.prepareStatement(
                "DELETE FROM Service WHERE id_service = ?")) {

            // Vérifier si le service a des commandes associées
            stmtCheck.setInt(1, idService);
            ResultSet rs = stmtCheck.executeQuery();
            rs.next();
            int count = rs.getInt(1);

            if (count > 0) {
                JOptionPane.showMessageDialog(this,
                        "Impossible de supprimer: ce service a des commandes associées",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Supprimer le service
            stmtDelete.setInt(1, idService);
            int rows = stmtDelete.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Service supprimé avec succès!");
                chargerServices();
                chargerTableauServices();
                viderFormulaire();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de suppression: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viderFormulaire() {
        txtId.setText("");
        txtNom.setText("");
        txtResponsable.setText("");
        txtLocalisation.setText("");
        txtTelephone.setText("");
        comboServices.setSelectedIndex(0);
    }

    private boolean validerFormulaire() {
        if (txtNom.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le nom du service est obligatoire",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!txtTelephone.getText().trim().isEmpty() && !txtTelephone.getText().trim().matches("^[0-9]{8}$")) {
            JOptionPane.showMessageDialog(this, "Le téléphone doit contenir 8 chiffres",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
}
