public class Produit {
    private String nom;
    private String description;
    private String type;       // consommable ou durable
    private String categorie;  // informatique, bureautique, etc.

    public Produit(String nom, String description, String type, String categorie) {
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.categorie = categorie;
    }

    // Getters
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getCategorie() { return categorie; }

    // Setters
    public void setNom(String nom) { this.nom = nom; }
    public void setDescription(String description) { this.description = description; }
    public void setType(String type) { this.type = type; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
}
