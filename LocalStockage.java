import java.time.LocalDateTime;
import java.util.ArrayList;

public class LocalStockage {
    private String reference;
    private LocalDateTime dateInventaire;
    private String emplacement; //
    private int capaciteMax;
    private String typeStockage;
    private Fournisseur fournisseurPrincipal;
    private ArrayList<Article> articles = new ArrayList<>();
    private String statut;

    // Constructeur simplifié
    public LocalStockage(String reference, String emplacement, int capaciteMax) {
        this.reference = reference;
        this.emplacement = emplacement;
        this.capaciteMax = capaciteMax;
        this.dateInventaire = LocalDateTime.now();
        this.statut = "ACTIF";
    }

    // Constructeur complet
    public LocalStockage(String reference, LocalDateTime dateInventaire,
                         String emplacement, int capaciteMax,
                         Fournisseur fournisseurPrincipal,
                         ArrayList<Article> articles, String statut) {
        this.reference = reference;
        this.dateInventaire = dateInventaire;
        this.emplacement = emplacement;
        this.capaciteMax = capaciteMax;
        this.fournisseurPrincipal = fournisseurPrincipal;
        this.articles = articles;
        this.statut = statut;
    }

    // Getters/Setters
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDateTime getDateInventaire() {
        return dateInventaire;
    }

    public void setDateInventaire(LocalDateTime dateInventaire) {
        this.dateInventaire = dateInventaire;
    }

    public String getEmplacement() {
        return emplacement;
    }

    public void setEmplacement(String emplacement) {
        this.emplacement = emplacement;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public String getTypeStockage() {
        return typeStockage;
    }

    public void setTypeStockage(String typeStockage) {
        this.typeStockage = typeStockage;
    }

    public Fournisseur getFournisseurPrincipal() {
        return fournisseurPrincipal;
    }

    public void setFournisseurPrincipal(Fournisseur fournisseurPrincipal) {
        this.fournisseurPrincipal = fournisseurPrincipal;
    }

    public ArrayList<Article> getArticles() {
        return articles;
    }

    public void setArticles(ArrayList<Article> articles) {
        this.articles = articles;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    // Méthode spécifique au stockage
    public void ajouterArticle(Article article) {
        if (articles.size() < capaciteMax) {
            articles.add(article);
        } else {
            System.out.println("Capacité maximale atteinte !");
        }
    }
}