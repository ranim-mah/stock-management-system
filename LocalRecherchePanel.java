import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class LocalRecherchePanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);
    private JTextField txtRecherche;
    private JComboBox<String> comboCritere;
    private JTable table;
    private DefaultTableModel tableModel;
    private Main main;

    public LocalRecherchePanel(Main main) {
        final Color MAIN_COLOR = new Color(12, 53, 106);
        final Color BG_COLOR = new Color(240, 240, 240, 200);
        this.main = main;
        setLayout(new BorderLayout(10, 10));
        initializeUI();
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

    private void initializeUI() {
        // Panel de recherche
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        txtRecherche = new JTextField(20);
        comboCritere = new JComboBox<>(new String[]{"Tous", "Code", "Nom", "Type", "Bâtiment", "Responsable"});

        JButton btnRechercher = new JButton("Rechercher");
        btnRechercher.addActionListener(e -> rechercherLocaux());

        JButton btnReset = new JButton("Réinitialiser");
        btnReset.addActionListener(e -> {
            txtRecherche.setText("");
            chargerTousLocaux();
        });

        searchPanel.add(new JLabel("Critère:"));
        searchPanel.add(comboCritere);
        searchPanel.add(new JLabel("Valeur:"));
        searchPanel.add(txtRecherche);
        searchPanel.add(btnRechercher);
        searchPanel.add(btnReset);

        // Tableau
        String[] columns = {"Code", "Nom", "Type", "Bâtiment", "Niveau", "Capacité", "Responsable"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Bouton de retour
        JButton btnRetour = new JButton("← Retour");
        btnRetour.addActionListener(e -> main.retourMenu());

        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(btnRetour, BorderLayout.SOUTH);

        chargerTousLocaux();
    }

    private void chargerTousLocaux() {
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

    private void rechercherLocaux() {
        String critere = (String) comboCritere.getSelectedItem();
        String valeur = txtRecherche.getText().trim();

        if (critere.equals("Tous")) {
            chargerTousLocaux();
            return;
        }

        String sql = "SELECT * FROM LocalStockage WHERE ";
        switch (critere) {
            case "Code":
                sql += "code_local LIKE ?";
                break;
            case "Nom":
                sql += "nom_local LIKE ?";
                break;
            case "Type":
                sql += "type_local LIKE ?";
                break;
            case "Bâtiment":
                sql += "batiment LIKE ?";
                break;
            case "Responsable":
                sql += "responsable LIKE ?";
                break;
        }

        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + valeur + "%");
            ResultSet rs = stmt.executeQuery();
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
            JOptionPane.showMessageDialog(main, "Erreur de recherche: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}