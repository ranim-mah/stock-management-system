import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GestionStockFrame extends JFrame implements ActionListener {
    private JComboBox<String> comboArticles, comboFournisseurs, comboLocaux;
    private JTextField txtQuantite, txtPrixUnitaire, txtReference;
    private JButton btnAjouterArticle, btnEntreeStock, btnSortieStock, btnInventaire, btnAlertes;
    private JCheckBox chkCritique;
    private JPanel mainPanel;

    public GestionStockFrame() {
        super("Gestion de Stock - ISIMM");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialisation des composants
        initComponents();
        setupLayout();
        loadDataFromDB();

        setVisible(true);
    }

    private void initComponents() {
        comboArticles = new JComboBox<>();
        comboFournisseurs = new JComboBox<>();
        comboLocaux = new JComboBox<>();

        txtReference = new JTextField(15);
        txtQuantite = new JTextField(10);
        txtPrixUnitaire = new JTextField(10);

        chkCritique = new JCheckBox("Article critique");

        btnAjouterArticle = new JButton("Nouvel Article");
        btnEntreeStock = new JButton("Entrée Stock");
        btnSortieStock = new JButton("Sortie Stock");
        btnInventaire = new JButton("Inventaire");
        btnAlertes = new JButton("Voir Alertes");

        // Ajout des listeners
        btnAjouterArticle.addActionListener(this);
        btnEntreeStock.addActionListener(this);
        btnSortieStock.addActionListener(this);
        btnInventaire.addActionListener(this);
        btnAlertes.addActionListener(this);
    }

    private void setupLayout() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Ligne 0
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Référence:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(txtReference, gbc);
        gbc.gridx = 2;
        mainPanel.add(chkCritique, gbc);

        // Ligne 1
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("Article:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(comboArticles, gbc);
        gbc.gridx = 2;
        mainPanel.add(btnAjouterArticle, gbc);

        // Ligne 2
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("Fournisseur:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(comboFournisseurs, gbc);

        // Ligne 3
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(new JLabel("Local de stockage:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(comboLocaux, gbc);

        // Ligne 4
        gbc.gridx = 0;
        gbc.gridy = 4;
        mainPanel.add(new JLabel("Quantité:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(txtQuantite, gbc);
        gbc.gridx = 2;
        mainPanel.add(new JLabel("Prix unitaire:"), gbc);
        gbc.gridx = 3;
        mainPanel.add(txtPrixUnitaire, gbc);

        // Ligne 5 - Boutons
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(btnEntreeStock);
        buttonPanel.add(btnSortieStock);
        buttonPanel.add(btnInventaire);
        buttonPanel.add(btnAlertes);
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel);
    }

    private void loadDataFromDB() {
        try (Connection conn = DatabaseConnector.getConnection()) {
            // Charger les articles
            PreparedStatement stmt = conn.prepareStatement("SELECT reference, nom FROM Article");
            ResultSet rs = stmt.executeQuery();
            comboArticles.removeAllItems();
            while (rs.next()) {
                comboArticles.addItem(rs.getString("reference") + " - " + rs.getString("nom"));
            }

            // Charger les fournisseurs
            stmt = conn.prepareStatement("SELECT id_fournisseur, nom FROM Fournisseur");
            rs = stmt.executeQuery();
            comboFournisseurs.removeAllItems();
            while (rs.next()) {
                comboFournisseurs.addItem(rs.getString("id_fournisseur") + " - " + rs.getString("nom")); // ✅
            }

            // Charger les locaux
            stmt = conn.prepareStatement("SELECT code_local, nom_local FROM LocalStockage");
            rs = stmt.executeQuery();
            comboLocaux.removeAllItems();
            while (rs.next()) {
                comboLocaux.addItem(rs.getString("code_local") + " - " + rs.getString("nom_local"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de chargement des données: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAjouterArticle) {
            ajouterArticle();
        } else if (e.getSource() == btnEntreeStock) {
            gererMouvementStock("ENTREE");
        } else if (e.getSource() == btnSortieStock) {
            gererMouvementStock("SORTIE");
        } else if (e.getSource() == btnInventaire) {
            new InventaireDialog(this).setVisible(true);
        } else if (e.getSource() == btnAlertes) {
            afficherAlertes();
        }
    }

    private void ajouterArticle() {
        String reference = txtReference.getText().trim();
        String nom = JOptionPane.showInputDialog(this, "Nom de l'article:");

        if (nom == null || nom.isEmpty()) return;

        try (Connection conn = DatabaseConnector.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO Article (reference, nom, critique) VALUES (?, ?, ?)");
            stmt.setString(1, reference);
            stmt.setString(2, nom);
            stmt.setBoolean(3, chkCritique.isSelected());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Article ajouté avec succès!");
                loadDataFromDB(); // Rafraîchir la liste
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur: " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void gererMouvementStock(String typeMouvement) {
        try {
            String articleSelectionne = (String) comboArticles.getSelectedItem();
            String reference = articleSelectionne.split(" - ")[0];
            int quantite = Integer.parseInt(txtQuantite.getText());
            double prixUnitaire = txtPrixUnitaire.getText().isEmpty() ? 0 : Double.parseDouble(txtPrixUnitaire.getText());
            String localSelectionne = (String) comboLocaux.getSelectedItem();
            String codeLocal = localSelectionne.split(" - ")[0];

            try (Connection conn = DatabaseConnector.getConnection()) {
                // Nouveau : Vérifier le stock avant une sortie
                if (typeMouvement.equals("SORTIE")) {
                    PreparedStatement checkStmt = conn.prepareStatement(
                            "SELECT quantite FROM Article WHERE reference = ?");
                    checkStmt.setString(1, reference);
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next() && rs.getInt("quantite") < quantite) {
                        JOptionPane.showMessageDialog(this,
                                "Stock insuffisant! Stock actuel: " + rs.getInt("quantite"),
                                "Erreur", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // Mettre à jour le stock seulement si la vérification est passée
                String updateSql = typeMouvement.equals("ENTREE") ?
                        "UPDATE Article SET quantite = quantite + ? WHERE reference = ?" :
                        "UPDATE Article SET quantite = quantite - ? WHERE reference = ?";

                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, quantite);
                    stmt.setString(2, reference);
                    stmt.executeUpdate();
                }

                // Enregistrer le mouvement
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO MouvementStock (type_mouvement, reference_article, quantite, prix_unitaire, code_local) " +
                                "VALUES (?, ?, ?, ?, ?)")) {
                    stmt.setString(1, typeMouvement);
                    stmt.setString(2, reference);
                    stmt.setInt(3, quantite);
                    stmt.setDouble(4, prixUnitaire);
                    stmt.setString(5, codeLocal);
                    stmt.executeUpdate();
                }

                JOptionPane.showMessageDialog(this,
                        typeMouvement.equals("ENTREE") ? "Entrée en stock enregistrée!" : "Sortie de stock enregistrée!");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer une quantité valide",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur: " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void afficherAlertes() {
        try (Connection conn = DatabaseConnector.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT reference, nom, quantite, seuil_alerte FROM Article " +
                            "WHERE quantite <= seuil_alerte OR date_peremption <= CURDATE() + INTERVAL 7 DAY");

            ResultSet rs = stmt.executeQuery();
            StringBuilder alertes = new StringBuilder("Articles en alerte:\n\n");

            while (rs.next()) {
                alertes.append(rs.getString("reference"))
                        .append(" - ")
                        .append(rs.getString("nom"))
                        .append(": Stock=")
                        .append(rs.getInt("quantite"))
                        .append("/")
                        .append(rs.getInt("seuil_alerte"))
                        .append("\n");
            }

            JOptionPane.showMessageDialog(this, alertes.toString(), "Alertes Stock", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur: " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GestionStockFrame());
    }
}

class InventaireDialog extends JDialog {
    public InventaireDialog(JFrame parent) {
        super(parent, "Gestion des Inventaires", true);
        setSize(500, 400);
        setLocationRelativeTo(parent);

        // Implémentez l'interface d'inventaire ici
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Fonctionnalité d'inventaire sera implémentée ici"), BorderLayout.CENTER);

        JButton btnFermer = new JButton("Fermer");
        btnFermer.addActionListener(e -> dispose());
        panel.add(btnFermer, BorderLayout.SOUTH);

        add(panel);
    }
}