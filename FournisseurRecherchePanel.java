import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class FournisseurRecherchePanel extends JPanel {
    private JTextField txtRecherche;
    private JComboBox<String> comboCritere;
    private JTable table;
    private DefaultTableModel tableModel;
    private Main main;
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    public FournisseurRecherchePanel(Main main) {
        this.main = main;
        setLayout(new BorderLayout(10, 10));
        initializeUI();
    }


    private void initializeUI() {
        // Panel de recherche
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        txtRecherche = new JTextField(20);
        comboCritere = new JComboBox<>(new String[]{"Tous", "ID", "Nom", "Adresse", "Téléphone", "Email", "Spécialité"});

        JButton btnRechercher = new JButton("Rechercher");
        btnRechercher.addActionListener(e -> rechercherFournisseurs());

        JButton btnReset = new JButton("Réinitialiser");
        btnReset.addActionListener(e -> {
            txtRecherche.setText("");
            chargerTousFournisseurs();
        });

        searchPanel.add(new JLabel("Critère:"));
        searchPanel.add(comboCritere);
        searchPanel.add(new JLabel("Valeur:"));
        searchPanel.add(txtRecherche);
        searchPanel.add(btnRechercher);
        searchPanel.add(btnReset);

        // Tableau des résultats
        String[] columns = {"ID", "Nom", "Adresse", "Téléphone", "Réputation"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(table);

        // Bouton de retour
        JButton btnRetour = new JButton("← Retour");
        btnRetour.addActionListener(e -> main.retourMenu());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(btnRetour, BorderLayout.WEST);

        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        chargerTousFournisseurs();
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

    private void chargerTousFournisseurs() {
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
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void rechercherFournisseurs() {
        String critere = (String) comboCritere.getSelectedItem();
        String valeur = txtRecherche.getText().trim();

        if (critere.equals("Tous")) {
            chargerTousFournisseurs();
            return;
        }

        String sql = "SELECT * FROM Fournisseur WHERE ";
        switch (critere) {
            case "ID":
                sql += "id_fournisseur LIKE ?";
                break;
            case "Nom":
                sql += "nom LIKE ?";
                break;
            case "Adresse":
                sql += "adresse LIKE ?";
                break;
            case "Téléphone":
                sql += "contact LIKE ?";
                break;
            case "Réputation":
                sql += "reputation LIKE ?";
                break;
        }
        sql += " ORDER BY nom";

        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (critere.equals("ID")) {
                stmt.setString(1, valeur);
            } else {
                stmt.setString(1, "%" + valeur + "%");
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("id_fournisseur"),
                        rs.getString("nom"),
                        rs.getString("adresse"),
                        rs.getString("contact"),
                        rs.getString("reputation")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(main, "Erreur de recherche: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}