import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class StockAlertesPanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    private JTable table;
    private DefaultTableModel model;
    private Main main;

    public StockAlertesPanel(Main mainFrame) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setOpaque(false);
        initializeUI();
    }


    private void initializeUI() {
        JPanel glassPanel = new JPanel(new BorderLayout());
        glassPanel.setBackground(BG_COLOR);
        glassPanel.setBorder(new CompoundBorder(
                new LineBorder(MAIN_COLOR, 2),
                new EmptyBorder(15, 15, 15, 15)
        ));

        String[] columns = {"Type Alerte", "Référence", "Nom", "Quantité", "Seuil", "Local"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 3 || column == 4 ? Integer.class : String.class;
            }
        };

        table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.setGridColor(MAIN_COLOR);
        table.setRowHeight(25);
        table.setDefaultRenderer(Object.class, new AlerteRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(MAIN_COLOR, 1));

        JButton btnRafraichir = createStyledButton("Rafraîchir");
        btnRafraichir.addActionListener(e -> chargerAlertes());

        JButton btnRetour = createStyledButton("← Retour");
        btnRetour.addActionListener(e -> main.retourMenu());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnRafraichir);
        buttonPanel.add(btnRetour);

        glassPanel.add(scrollPane, BorderLayout.CENTER);
        glassPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(glassPanel, BorderLayout.CENTER);
        chargerAlertes();
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

    private void chargerAlertes() {
        model.setRowCount(0);
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM VueAlertesStock")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("type_alerte"),
                        rs.getString("reference"),
                        rs.getString("nom"),
                        rs.getInt("quantite"),
                        rs.getInt("seuil_alerte"),
                        rs.getString("nom_local")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class AlerteRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String typeAlerte = (String) table.getValueAt(row, 0);

            switch (typeAlerte) {
                case "RUPTURE_STOCK":
                    c.setBackground(new Color(255, 150, 150)); // Rouge
                    break;
                case "RUPTURE_CRITIQUE":
                    c.setBackground(new Color(255, 255, 150)); // Jaune
                    break;
                case "PERIMITION_PROCHE":
                    c.setBackground(new Color(150, 255, 150)); // Vert
                    break;
                default:
                    c.setBackground(table.getBackground());
            }
            return c;
        }
    }
}