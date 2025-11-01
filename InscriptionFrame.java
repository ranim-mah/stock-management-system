import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class InscriptionFrame extends JFrame {
    private JTextField txtUsername = new JTextField();
    private JPasswordField txtPassword = new JPasswordField();
    private JTextField txtEmail = new JTextField();
    private final Color MAIN_COLOR = new Color(40, 113, 201);

    public InscriptionFrame() {
        setTitle("Création de compte");
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);

        // Titre
        JLabel lblTitle = new JLabel("Créer un compte");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        lblTitle.setForeground(MAIN_COLOR);
        gbc.gridy = 0;
        add(lblTitle, gbc);

        // Formulaire
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);

        addInputField("Nom d'utilisateur", txtUsername, formPanel);
        addInputField("Mot de passe", txtPassword, formPanel);
        addInputField("Email", txtEmail, formPanel);

        // Bouton d'inscription
        JButton btnRegister = createPrimaryButton("S'inscrire", 200, 40);
        btnRegister.addActionListener(this::creerCompte);

        gbc.gridy = 1;
        add(formPanel, gbc);
        gbc.gridy = 2;
        add(btnRegister, gbc);

        setVisible(true);
    }

    private void addInputField(String label, JComponent field, Container parent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.PLAIN, 14));

        field.setPreferredSize(new Dimension(300, 35));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10))
        );

        panel.add(lbl, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        parent.add(panel);
    }

    private JButton createPrimaryButton(String text, int width, int height) {
        JButton btn = new JButton(text);
        btn.setBackground(MAIN_COLOR);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(width, height));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        btn.setFocusPainted(false);
        return btn;
    }

    private void creerCompte(ActionEvent e) {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        String email = txtEmail.getText();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez remplir tous les champs",
                    "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, 'gestionnaire')")) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, email);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this,
                        "Compte créé avec succès!",
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
            }
        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(this,
                    "Nom d'utilisateur ou email déjà existant",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Erreur de création: " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}