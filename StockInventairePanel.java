import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;

public class StockInventairePanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtLocal;

    public StockInventairePanel() {
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

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topPanel.setOpaque(false);

        txtLocal = createStyledTextField();
        JButton btnCharger = createStyledButton("Charger");
        JButton btnValider = createStyledButton("Valider");

        btnCharger.addActionListener(e -> chargerArticles());
        btnValider.addActionListener(e -> validerInventaire());

        topPanel.add(createStyledLabel("Local:"));
        topPanel.add(txtLocal);
        topPanel.add(btnCharger);

        String[] columns = {"Référence", "Nom", "Stock théorique", "Stock réel", "Écart"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 4 ? Integer.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };

        table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.setGridColor(MAIN_COLOR);
        table.getColumnModel().getColumn(4).setCellRenderer(new EcartRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(MAIN_COLOR, 1));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.add(btnValider);

        glassPanel.add(topPanel, BorderLayout.NORTH);
        glassPanel.add(scrollPane, BorderLayout.CENTER);
        glassPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(glassPanel, BorderLayout.CENTER);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField(15);
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

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(MAIN_COLOR);
        return label;
    }

    private void chargerArticles() {
        model.setRowCount(0);
        String local = txtLocal.getText().trim();

        if (local.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez spécifier un local", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "SELECT reference, nom, quantite FROM Article WHERE code_local = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, local);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("reference"),
                        rs.getString("nom"),
                        rs.getInt("quantite"),
                        "",
                        0
                });
            }
            model.fireTableDataChanged();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur de chargement: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void validerInventaire() {
        String local = txtLocal.getText().trim();
        if (local.isEmpty()) return;

        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);

            for (int i = 0; i < model.getRowCount(); i++) {
                String reference = (String) model.getValueAt(i, 0);
                int stockTheorique = Integer.parseInt(model.getValueAt(i, 2).toString());
                int stockReel = Integer.parseInt(model.getValueAt(i, 3).toString());
                int ecart = stockReel - stockTheorique;

                model.setValueAt(ecart, i, 4);

                // Mise à jour du stock
                PreparedStatement stmtUpdate = conn.prepareStatement(
                        "UPDATE Article SET quantite = ? WHERE reference = ?");
                stmtUpdate.setInt(1, stockReel);
                stmtUpdate.setString(2, reference);
                stmtUpdate.executeUpdate();

                if (ecart != 0) {
                    PreparedStatement stmtInsert = conn.prepareStatement(
                            "INSERT INTO MouvementStock (type, reference_article, quantite, date, notes) " +
                                    "VALUES ('AJUSTEMENT', ?, ?, ?, ?)");
                    stmtInsert.setString(1, reference);
                    stmtInsert.setInt(2, ecart);
                    stmtInsert.setDate(3, Date.valueOf(LocalDate.now()));
                    stmtInsert.setString(4, "Ajustement d'inventaire - Local: " + local);
                    stmtInsert.executeUpdate();
                }
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Inventaire validé avec succès!");
        } catch (SQLException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }


    private static class EcartRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int ecart = 0;
            try {
                ecart = Integer.parseInt(value.toString());
            } catch (Exception e) {
            }

            if (ecart > 0) c.setBackground(new Color(200, 255, 200));
            else if (ecart < 0) c.setBackground(new Color(255, 200, 200));
            else c.setBackground(table.getBackground());
            return c;
        }
    }
}