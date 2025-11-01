import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Fournisseur {
    private String codeFournisseur;
    private String nom;
    private String contact;
    private String email;
    private ArrayList<Article> articlesFournis;
    private Connection connection;

    public Fournisseur(String codeFournisseur, String nom, String contact, String email) {
        this.codeFournisseur = codeFournisseur;
        this.nom = nom;
        this.contact = contact;
        this.email = email;
        this.articlesFournis = new ArrayList<>();
    }

    // Getters et Setters
    public String getCodeFournisseur() {
        return codeFournisseur;
    }

    public void setCodeFournisseur(String codeFournisseur) {
        this.codeFournisseur = codeFournisseur;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ArrayList<Article> getArticlesFournis() {
        return articlesFournis;
    }

    // Méthodes métier
    public void ajouterFournisseur() throws SQLException {
        connection = DatabaseConnector.getConnection();
        String query = "INSERT INTO Fournisseur VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, codeFournisseur);
            ps.setString(2, nom);
            ps.setString(3, contact);
            ps.setString(4, email);
            ps.executeUpdate();
        }
    }

    public void ajouterArticleFourni(Article article) {
        articlesFournis.add(article);
    }

    public boolean fournisseurExisteDeja() throws SQLException {
        connection = DatabaseConnector.getConnection();
        String query = "SELECT COUNT(*) FROM Fournisseur WHERE code_fournisseur = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, codeFournisseur);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    public void mettreAJourContact(String nouveauContact) throws SQLException {
        connection = DatabaseConnector.getConnection();
        String query = "UPDATE Fournisseur SET contact = ? WHERE code_fournisseur = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, nouveauContact);
            ps.setString(2, codeFournisseur);
            ps.executeUpdate();
        }
    }
}