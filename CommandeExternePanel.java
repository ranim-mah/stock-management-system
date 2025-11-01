import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CommandeExternePanel extends JPanel {
    private JTextField txtNumCommande, txtDate, txtQuantite, txtPrixUnitaire;
    private JComboBox<String> comboFournisseurs, comboArticles;
    private JButton btnAjouter, btnValider, btnAnnuler;
    private DefaultTableModel tableModel;
    private JTable table;
    private Main main;
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);


    public CommandeExternePanel(Main main) {
        this.main = main;
        setLayout(new BorderLayout(10, 10));
        initComponents();
        setupLayout();
        loadDataFromDB();
    }


    private void initComponents() {
        txtNumCommande = new JTextField(genererNumeroCommande());
        txtNumCommande.setEditable(false);
        txtDate = new JTextField(LocalDate.now().toString());
        txtDate.setEditable(false);
        txtQuantite = new JTextField();
        txtPrixUnitaire = new JTextField();

        comboFournisseurs = new JComboBox<>();
        comboArticles = new JComboBox<>();

        btnAjouter = new JButton("Ajouter au Bon");
        btnValider = new JButton("Valider Commande");
        btnAnnuler = new JButton("Annuler");

        // Configuration des écouteurs
        btnAjouter.addActionListener(e -> ajouterLigne());
        btnValider.addActionListener(e -> validerCommande());
        btnAnnuler.addActionListener(e -> main.retourMenu());
    }

    private void setupLayout() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Détails de la Commande"));

        formPanel.add(new JLabel("Numéro Commande:"));
        formPanel.add(txtNumCommande);
        formPanel.add(new JLabel("Date:"));
        formPanel.add(txtDate);
        formPanel.add(new JLabel("Fournisseur*:"));
        formPanel.add(comboFournisseurs);
        formPanel.add(new JLabel("Article*:"));
        formPanel.add(comboArticles);
        formPanel.add(new JLabel("Quantité*:"));
        formPanel.add(txtQuantite);
        formPanel.add(new JLabel("Prix unitaire*:"));
        formPanel.add(txtPrixUnitaire);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(btnAjouter);
        buttonPanel.add(btnValider);
        buttonPanel.add(btnAnnuler);

        String[] columns = {"Référence", "Article", "Quantité", "Prix unitaire", "Total"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 4 ? Double.class : Object.class;
            }
        };

        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(formPanel, BorderLayout.CENTER);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
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

    private void loadDataFromDB() {
        chargerFournisseurs();
        chargerArticles();
    }

    private void chargerFournisseurs() {
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_fournisseur, nom FROM Fournisseur")) {

            comboFournisseurs.removeAllItems();
            while (rs.next()) {
                comboFournisseurs.addItem(rs.getString("id_fournisseur") + " - " + rs.getString("nom"));
            }
        } catch (SQLException e) {
            showError("Erreur de chargement des fournisseurs : " + e.getMessage());
        }
    }

    private void chargerArticles() {
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT reference, nom FROM Article")) {

            comboArticles.removeAllItems();
            while (rs.next()) {
                comboArticles.addItem(rs.getString("reference") + " - " + rs.getString("nom"));
            }
        } catch (SQLException e) {
            showError("Erreur de chargement des articles : " + e.getMessage());
        }
    }

    private void ajouterLigne() {
        try {
            String article = (String) comboArticles.getSelectedItem();
            int quantite = Integer.parseInt(txtQuantite.getText());
            double prix = Double.parseDouble(txtPrixUnitaire.getText());

            String reference = article.split(" - ")[0];
            String nomArticle = article.split(" - ")[1];

            tableModel.addRow(new Object[]{
                    reference,
                    nomArticle,
                    quantite,
                    prix,
                    quantite * prix
            });

            txtQuantite.setText("");
            txtPrixUnitaire.setText("");
        } catch (Exception e) {
            showError("Veuillez vérifier les valeurs saisies !");
        }
    }

    private void validerCommande() {
        if (tableModel.getRowCount() == 0 || comboFournisseurs.getSelectedIndex() == -1) {
            showError("Veuillez remplir tous les champs obligatoires !");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);

            String idFournisseur = ((String) comboFournisseurs.getSelectedItem()).split(" - ")[0];

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO Commande (type_commande, reference_article, id_fournisseur, quantite, date_commande, statut) VALUES (?, ?, ?, ?, ?, ?)")) {

                    String referenceArticle = (String) tableModel.getValueAt(i, 0);
                    int quantite = (Integer) tableModel.getValueAt(i, 2);
                    double prix = (Double) tableModel.getValueAt(i, 3);

                    stmt.setString(1, "EXTERNE");
                    stmt.setString(2, referenceArticle);
                    stmt.setString(3, idFournisseur);
                    stmt.setInt(4, quantite);
                    stmt.setDate(5, Date.valueOf(LocalDate.now()));
                    stmt.setString(6, "EN_ATTENTE");
                    stmt.executeUpdate();
                }
            }

            conn.commit();
            JOptionPane.showMessageDialog(main, "Commande validée avec succès !");
            refreshInterface();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main, "Erreur d'enregistrement : " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(main, "Erreur inattendue : " + e.getMessage());
        }
    }

    private void refreshInterface() {
        tableModel.setRowCount(0);
        comboFournisseurs.setSelectedIndex(-1);
        comboArticles.setSelectedIndex(-1);
        txtNumCommande.setText(genererNumeroCommande());
    }

    private String genererNumeroCommande() {
        return "CMD-EXT-" + System.currentTimeMillis();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(main, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}