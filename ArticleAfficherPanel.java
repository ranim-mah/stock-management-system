import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class ArticleAfficherPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Font HEADER_FONT = new Font("Arial", Font.BOLD, 18);
    private JTable table;
    private DefaultTableModel tableModel;
    private final Runnable onBack;

    public ArticleAfficherPanel(Runnable onBack) {
        this.onBack = onBack;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(240, 240, 240));

        // Configuration du tableau
        String[] columns = {"Référence", "Nom", "Description", "Quantité", "Prix", "Seuil", "Type", "Catégorie"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        styleTable();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(MAIN_COLOR, 2));

        // Bouton de retour stylisé
        JButton btnRetour = createStyledButton("← Retour au menu", MAIN_COLOR);
        btnRetour.addActionListener(e -> onBack.run());

        add(scrollPane, BorderLayout.CENTER);
        add(btnRetour, BorderLayout.SOUTH);

        chargerArticles();
    }

    private void styleTable() {
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(30);

        // Style des en-têtes
        JTableHeader header = table.getTableHeader();
        header.setFont(HEADER_FONT);
        header.setBackground(MAIN_COLOR);
        header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createLineBorder(MAIN_COLOR, 2));
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 1),
                new EmptyBorder(10, 25, 10, 25)
        ));
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

    // Remplace le début de cette méthode :
    public void chargerArticles() {
        System.out.println("Début du chargement des articles...");

        tableModel.setRowCount(0);

        String query = "SELECT reference, nom, description, quantite, prix_unitaire, seuil_alerte, type, categorie FROM Article";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                Object[] row = new Object[]{
                        rs.getString("reference"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getInt("quantite"),
                        rs.getDouble("prix_unitaire"),
                        rs.getInt("seuil_alerte"),
                        rs.getString("type"),
                        rs.getString("categorie")
                };
                tableModel.addRow(row);
                count++;
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "<html><b>Erreur de chargement :</b><br>" + e.getMessage() + "</html>",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


}