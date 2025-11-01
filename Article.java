import java.sql.*;
import java.time.LocalDate;

public class Article extends Produit {
    private String reference;
    private int quantite;
    private int stockMinimal;
    private double prixUnitaire;
    private String emplacement;
    private LocalDate datePeremption;
    private boolean critique;

    public Article(String nom, String description, String type, String categorie,
                   String reference, int quantite, int stockMinimal,
                   double prixUnitaire, String emplacement,
                   LocalDate datePeremption, boolean critique) {
        super(nom, description, type, categorie);
        this.reference = reference;
        this.quantite = quantite;
        this.stockMinimal = stockMinimal;
        this.prixUnitaire = prixUnitaire;
        this.emplacement = emplacement;
        this.datePeremption = datePeremption;
        this.critique = critique;
    }

    // ======= GETTERS & SETTERS =======
    public String getReference() { return reference; }
    public int getQuantite() { return quantite; }
    public int getStockMinimal() { return stockMinimal; }
    public double getPrixUnitaire() { return prixUnitaire; }
    public String getEmplacement() { return emplacement; }
    public LocalDate getDatePeremption() { return datePeremption; }
    public boolean isCritique() { return critique; }

    public void setQuantite(int quantite) { this.quantite = quantite; }
    public void setStockMinimal(int stockMinimal) { this.stockMinimal = stockMinimal; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public void setEmplacement(String emplacement) { this.emplacement = emplacement; }
    public void setDatePeremption(LocalDate datePeremption) { this.datePeremption = datePeremption; }
    public void setCritique(boolean critique) { this.critique = critique; }

    // ======= MÉTHODES MÉTIER =======

    // Vérifie si l'article existe
    public boolean articleExisteDeja(Connection conn) throws SQLException {
        String query = "SELECT COUNT(*) FROM Article WHERE reference = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, reference);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    // Ajouter article
    public void ajouterArticle() throws SQLException, ArticleDejaExistantException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            if (!articleExisteDeja(conn)) {
                String query = "INSERT INTO Article (nom, description, type, categorie, reference, quantite, stock_minimal, prix_unitaire, emplacement, date_peremption, critique) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, getNom());
                    ps.setString(2, getDescription());
                    ps.setString(3, getType());
                    ps.setString(4, getCategorie());
                    ps.setString(5, reference);
                    ps.setInt(6, quantite);
                    ps.setInt(7, stockMinimal);
                    ps.setDouble(8, prixUnitaire);
                    ps.setString(9, emplacement);
                    ps.setDate(10, Date.valueOf(datePeremption));
                    ps.setBoolean(11, critique);
                    ps.executeUpdate();
                    System.out.println("✅ Article ajouté.");
                }
            } else {
                throw new ArticleDejaExistantException("❗ Cet article existe déjà !");
            }
        }
    }

    // Modifier article
    public void modifierArticle() throws SQLException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String query = "UPDATE Article SET nom = ?, description = ?, type = ?, categorie = ?, quantite = ?, stock_minimal = ?, prix_unitaire = ?, emplacement = ?, date_peremption = ?, critique = ? WHERE reference = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, getNom());
                ps.setString(2, getDescription());
                ps.setString(3, getType());
                ps.setString(4, getCategorie());
                ps.setInt(5, quantite);
                ps.setInt(6, stockMinimal);
                ps.setDouble(7, prixUnitaire);
                ps.setString(8, emplacement);
                ps.setDate(9, Date.valueOf(datePeremption));
                ps.setBoolean(10, critique);
                ps.setString(11, reference);
                ps.executeUpdate();
                System.out.println("✅ Article modifié.");
            }
        }
    }

    // Supprimer article
    public void supprimerArticle() throws SQLException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String query = "DELETE FROM Article WHERE reference = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, reference);
                ps.executeUpdate();
                System.out.println("🗑️ Article supprimé.");
            }
        }
    }

    // Articles en alerte (périmés bientôt ou en stock critique)
    public static void verifierAlertes() throws SQLException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String query = "SELECT * FROM Article WHERE date_peremption <= ? OR quantite <= stock_minimal OR critique = true";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setDate(1, Date.valueOf(LocalDate.now().plusDays(7))); // articles à périmer dans 7 jours
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    System.out.println("⚠️ ALERTE STOCK : Article [" + rs.getString("nom") + "] référence [" + rs.getString("reference") + "]");
                }
            }
        }
    }
}
