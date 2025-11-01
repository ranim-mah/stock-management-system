import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDate;

public class CommandeInternePanel extends JPanel {
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private final Color BG_COLOR = new Color(240, 240, 240, 200);

    private JTextField txtNumCommande, txtDate, txtQuantite;
    private JComboBox<String> comboServices, comboArticles, comboLocaux;
    private JButton btnAjouter, btnValider, btnAnnuler;
    private DefaultTableModel tableModel;
    private JTable table;
    private Main main;

    public CommandeInternePanel(Main main) {
        this.main = main;
        setLayout(new BorderLayout(10, 10));
        initComponents();
        setupLayout();
        loadInitialData();
    }


    private void initComponents() {
        txtNumCommande = new JTextField(genererNumeroCommande());
        txtNumCommande.setEditable(false);
        txtDate = new JTextField(LocalDate.now().toString());
        txtDate.setEditable(false);
        txtQuantite = createStyledTextField();

        comboServices = new JComboBox<>();
        comboArticles = new JComboBox<>();
        comboLocaux = new JComboBox<>();

        btnAjouter = createStyledButton("Ajouter au Bon");
        btnValider = createStyledButton("Valider Commande");
        btnAnnuler = createStyledButton("Annuler");

        btnAjouter.addActionListener(e -> ajouterLigne());
        btnValider.addActionListener(e -> validerCommande());
        btnAnnuler.addActionListener(e -> main.retourMenu());

        comboArticles.addActionListener(e -> chargerLocauxPourArticle());
    }

    private void setupLayout() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Détails de la Commande"));

        formPanel.add(new JLabel("Numéro Commande:"));
        formPanel.add(txtNumCommande);
        formPanel.add(new JLabel("Date:"));
        formPanel.add(txtDate);
        formPanel.add(new JLabel("Service*:"));
        formPanel.add(comboServices);
        formPanel.add(new JLabel("Article*:"));
        formPanel.add(comboArticles);
        formPanel.add(new JLabel("Local*:"));
        formPanel.add(comboLocaux);
        formPanel.add(new JLabel("Quantité*:"));
        formPanel.add(txtQuantite);


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(btnAjouter);
        buttonPanel.add(btnValider);
        buttonPanel.add(btnAnnuler);

        String[] columns = {"Référence", "Local", "Article", "Quantité"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return Integer.class;
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

    private void loadInitialData() {
        chargerServices();
        chargerArticles();
    }

    private void chargerServices() {
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_service, nom FROM Service")) {

            comboServices.removeAllItems();
            while (rs.next()) {
                comboServices.addItem(rs.getInt("id_service") + " - " + rs.getString("nom"));
            }
        } catch (SQLException e) {
            showError("Erreur de chargement des services : " + e.getMessage());
        }
    }

    private void chargerArticles() {
        comboArticles.removeAllItems();

        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT reference, nom FROM Article")) { // Utilisation de DISTINCT

            while (rs.next()) {
                String articleEntry = rs.getString("reference") + " - " + rs.getString("nom");
                comboArticles.addItem(articleEntry);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(main,
                    "Erreur de chargement des articles : " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }

        comboArticles.revalidate();
        comboArticles.repaint();
    }

    private void chargerLocauxPourArticle() {
        String selected = (String) comboArticles.getSelectedItem();
        if (selected == null) return;

        String reference = selected.split(" - ")[0];
        comboLocaux.removeAllItems();

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT code_local FROM Article WHERE reference = ?")) {

            stmt.setString(1, reference);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                comboLocaux.addItem(rs.getString("code_local"));
            }
        } catch (SQLException e) {
            showError("Erreur de chargement des locaux : " + e.getMessage());
        }
    }

    private void ajouterLigne() {
        try {
            String article = (String) comboArticles.getSelectedItem();
            String local = (String) comboLocaux.getSelectedItem();
            int quantite = Integer.parseInt(txtQuantite.getText());

            String reference = article.split(" - ")[0];
            String nomArticle = article.split(" - ")[1];

            tableModel.addRow(new Object[]{
                    reference,
                    local,
                    nomArticle,
                    quantite,
            });

            txtQuantite.setText("");
        } catch (Exception e) {
            showError("Veuillez vérifier les valeurs saisies !");
        }
    }

    private void validerCommande() {
        if (tableModel.getRowCount() == 0 || comboServices.getSelectedIndex() == -1) {
            showError("Veuillez remplir tous les champs obligatoires !");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);

            int idService = Integer.parseInt(((String) comboServices.getSelectedItem()).split(" - ")[0]);

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String reference = (String) tableModel.getValueAt(i, 0);
                String codeLocal = (String) tableModel.getValueAt(i, 1);
                int quantite = (Integer) tableModel.getValueAt(i, 3);
                double prix = getPrixArticle(reference, codeLocal);

                // Vérification unique du stock
                if (!verifierStock(reference, codeLocal, quantite)) {
                    conn.rollback();
                    return;
                }

                // Mise à jour unique du stock
                mettreAJourStock(reference, codeLocal, quantite);

                // Insertion unique de la commande
                insererCommande(conn, idService, reference, codeLocal, quantite, prix);
            }

            conn.commit();
            JOptionPane.showMessageDialog(main, "Commande validée avec succès !");
            refreshInterface();

        } catch (SQLException e) {
            showError("Erreur d'enregistrement : " + e.getMessage());
        } catch (Exception e) {
            showError("Erreur inattendue : " + e.getMessage());
        }
    }

    private double getPrixArticle(String reference, String codeLocal) throws SQLException {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT prix_unitaire FROM Article WHERE reference = ? AND code_local = ?")) {

            stmt.setString(1, reference);
            stmt.setString(2, codeLocal);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("prix_unitaire");
            }
            throw new SQLException("Prix non trouvé pour l'article " + reference);
        }
    }

    private boolean verifierStock(String reference, String codeLocal, int quantite) throws SQLException {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT quantite FROM Article WHERE reference = ? AND code_local = ?")) {

            stmt.setString(1, reference);
            stmt.setString(2, codeLocal);
            ResultSet rs = stmt.executeQuery();

            if (rs.next() && rs.getInt("quantite") >= quantite) {
                return true;
            }
            showError("Stock insuffisant pour l'article " + reference + " dans le local " + codeLocal);
            return false;
        }
    }

    private void mettreAJourStock(String reference, String codeLocal, int quantite) throws SQLException {
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Article SET quantite = quantite - ? WHERE reference = ? AND code_local = ?")) {

            stmt.setInt(1, quantite);
            stmt.setString(2, reference);
            stmt.setString(3, codeLocal);
            stmt.executeUpdate();
        }
    }

    private void insererCommande(Connection conn, int idService, String reference,
                                 String codeLocal, int quantite, double prix) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Commande (type_commande, reference_article, id_service, quantite, date_commande, statut) " +
                        "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, "INTERNE");
            stmt.setString(2, reference);
            stmt.setInt(3, idService);
            stmt.setInt(4, quantite);
            stmt.setDate(5, Date.valueOf(LocalDate.now()));
            stmt.setString(6, "VALIDE");
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                insererLigneCommande(conn, rs.getInt(1), reference, codeLocal, quantite);
                insererMouvementStock(conn, reference, codeLocal, quantite, prix);
            }
        }
    }

    private void insererLigneCommande(Connection conn, int idCommande, String reference,
                                      String codeLocal, int quantite) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO LigneCommandeInterne (id_commande, reference_article, code_local, quantite) " +
                        "VALUES (?, ?, ?, ?)")) {

            stmt.setInt(1, idCommande);
            stmt.setString(2, reference);
            stmt.setString(3, codeLocal);
            stmt.setInt(4, quantite);
            stmt.executeUpdate();
        }
    }

    private void insererMouvementStock(Connection conn, String reference, String codeLocal,
                                       int quantite, double prix) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO MouvementStock (type_mouvement, reference_article, quantite, prix_unitaire, code_local, date_mouvement) " +
                        "VALUES (?, ?, ?, ?, ?, ?)")) {

            stmt.setString(1, "SORTIE");
            stmt.setString(2, reference);
            stmt.setInt(3, quantite);
            stmt.setDouble(4, prix);
            stmt.setString(5, codeLocal);
            stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
        }
    }

    private void refreshInterface() {
        tableModel.setRowCount(0);
        comboServices.setSelectedIndex(-1);
        comboArticles.setSelectedIndex(-1);
        comboLocaux.removeAllItems();
        txtNumCommande.setText(genererNumeroCommande());
    }

    private String genererNumeroCommande() {
        return "CMD-INT-" + System.currentTimeMillis();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(main, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}