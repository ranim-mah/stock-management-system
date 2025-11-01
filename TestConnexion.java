public class TestConnexion {
    public static void main(String[] args) {
        try {
            java.sql.Connection conn = DatabaseConnector.getConnection();
            System.out.println("Connexion OK !");
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}
