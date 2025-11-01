import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;


public class ServiceRecherchePanel extends JPanel {
    private JTextField txtRecherche;
    private JComboBox<String> comboCritere;
    private JTable table;
    private DefaultTableModel tableModel;
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);


    public ServiceRecherchePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de recherche
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        txtRecherche = new JTextField(20);
        comboCritere = new JComboBox<>(new String[]{"Tous", "ID", "Nom", "Responsable", "Localisation", "Téléphone"});

        JButton btnRechercher = new JButton("Rechercher");
        JButton btnReset = new JButton("Réinitialiser");

        btnRechercher.addActionListener(e -> rechercherServices());
        btnReset.addActionListener(e -> {
            txtRecherche.setText("");
            chargerTousServices();
        });

        searchPanel.add(new JLabel("Critère:"));
        searchPanel.add(comboCritere);
        searchPanel.add(new JLabel("Valeur:"));
        searchPanel.add(txtRecherche);
        searchPanel.add(btnRechercher);
        searchPanel.add(btnReset);

        // Tableau
        String[] columns = {"ID", "Nom", "Responsable", "Localisation", "Téléphone"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);

        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        chargerTousServices();
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

    private void chargerTousServices() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Service ORDER BY nom")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id_service"),
                        rs.getString("nom"),
                        rs.getString("responsable"),
                        rs.getString("localisation"),
                        rs.getString("telephone")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rechercherServices() {
        String critere = (String) comboCritere.getSelectedItem();
        String valeur = txtRecherche.getText().trim();

        if (critere.equals("Tous")) {
            chargerTousServices();
            return;
        }

        String sql = "SELECT * FROM Service WHERE ";
        switch (critere) {
            case "ID":
                sql += "id_service = ?";
                break;
            case "Nom":
                sql += "nom LIKE ?";
                break;
            case "Responsable":
                sql += "responsable LIKE ?";
                break;
            case "Localisation":
                sql += "localisation LIKE ?";
                break;
            case "Téléphone":
                sql += "telephone LIKE ?";
                break;
        }
        sql += " ORDER BY nom";

        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (critere.equals("ID")) {
                stmt.setInt(1, Integer.parseInt(valeur));
            } else {
                stmt.setString(1, "%" + valeur + "%");
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id_service"),
                        rs.getString("nom"),
                        rs.getString("responsable"),
                        rs.getString("localisation"),
                        rs.getString("telephone")
                });
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "L'ID doit être un nombre valide",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur de recherche: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}