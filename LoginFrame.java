import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Random;

import org.mindrot.jbcrypt.BCrypt;

public class LoginFrame extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel cards = new JPanel(cardLayout);
    private JTextField txtUsername, txtEmail, txtVerificationCode, txtNewUser, txtRegisterEmail;
    private JPasswordField txtPassword, txtNewPassword, txtRegisterPass;
    private final Color MAIN_COLOR = new Color(12, 53, 106);
    private Image backgroundImage;
    private Runnable onSuccessCallback;
    private int loginAttempts = 0;
    private String generatedCode;
    private Timer lockTimer;
    private String loggedInUserRole;

    public LoginFrame(Runnable onSuccessCallback) {
        this.onSuccessCallback = onSuccessCallback;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Connexion - Gestion Stock ISIMM");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            backgroundImage = new ImageIcon(getClass().getResource("/login10.jpg")).getImage();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Image de fond non trouvée");
        }

        JPanel backgroundPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };

        cards.setOpaque(false);
        cards.add(createLoginPanel(), "login");
        cards.add(createPasswordResetPanel(), "reset");
        cards.add(createRegistrationPanel(), "register");
        cards.add(createLockedPanel(), "locked");

        backgroundPanel.add(cards);
        add(backgroundPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        JLabel lblTitle = new JLabel("CONNEXION");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 28));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtUsername = createTextFieldWithPlaceholder("Nom d'utilisateur");
        txtPassword = createPasswordFieldWithPlaceholder("Mot de passe");

        JPanel whiteRectangle = new JPanel();
        whiteRectangle.setMaximumSize(new Dimension(400, 50)); // largeur du champ, hauteur personnalisée
        whiteRectangle.setBackground(Color.WHITE);
        whiteRectangle.setBorder(BorderFactory.createLineBorder(new Color(12, 53, 106)));
        whiteRectangle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnLogin = new JButton("SE CONNECTER");
        styleMainButton(btnLogin);
        btnLogin.addActionListener(e -> handleLogin());

        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        linkPanel.setOpaque(false);
        JButton btnForgot = createLinkButton("Mot de passe oublié ?");
        JButton btnRegister = createLinkButton("Créer un compte");

        btnForgot.addActionListener(e -> cardLayout.show(cards, "reset"));
        btnRegister.addActionListener(e -> cardLayout.show(cards, "register"));

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(40));
        panel.add(txtUsername);
        panel.add(Box.createVerticalStrut(20));
        panel.add(txtPassword);
        panel.add(Box.createVerticalStrut(40));
        panel.add(btnLogin);
        panel.add(Box.createVerticalStrut(20));
        linkPanel.add(btnForgot);
        linkPanel.add(btnRegister);
        panel.add(linkPanel);

        return panel;
    }

    private JPanel createRegistrationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        JLabel lblTitle = new JLabel("CRÉATION DE COMPTE");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtNewUser = createTextFieldWithPlaceholder("Nom d'utilisateur");
        txtRegisterEmail = createTextFieldWithPlaceholder("Adresse email");
        txtRegisterPass = createPasswordFieldWithPlaceholder("Mot de passe (8 caractères min)");

        JButton btnRegister = new JButton("S'INSCRIRE");
        JButton btnBack = createLinkButton("← Retour à la connexion");

        styleMainButton(btnRegister);

        btnRegister.addActionListener(e -> handleRegistration());
        btnBack.addActionListener(e -> cardLayout.show(cards, "login"));

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(20));
        panel.add(txtNewUser);
        panel.add(Box.createVerticalStrut(10));
        panel.add(txtRegisterEmail);
        panel.add(Box.createVerticalStrut(10));
        panel.add(txtRegisterPass);
        panel.add(Box.createVerticalStrut(20));
        panel.add(btnRegister);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnBack);

        return panel;
    }

    private JPanel createPasswordResetPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        JLabel lblTitle = new JLabel("RÉINITIALISATION DU MOT DE PASSE");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtEmail = createTextFieldWithPlaceholder("Email enregistré");
        txtVerificationCode = createTextFieldWithPlaceholder("Code de vérification");
        txtNewPassword = createPasswordFieldWithPlaceholder("Nouveau mot de passe");

        JButton btnSendCode = new JButton("ENVOYER LE CODE");
        JButton btnSubmit = new JButton("VALIDER");
        JButton btnBack = createLinkButton("← Retour à la connexion");

        styleMainButton(btnSendCode);
        styleMainButton(btnSubmit);

        btnSendCode.addActionListener(e -> handleSendCode());
        btnSubmit.addActionListener(e -> handlePasswordReset());
        btnBack.addActionListener(e -> cardLayout.show(cards, "login"));

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(20));
        panel.add(txtEmail);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnSendCode);
        panel.add(Box.createVerticalStrut(20));
        panel.add(txtVerificationCode);
        panel.add(Box.createVerticalStrut(10));
        panel.add(txtNewPassword);
        panel.add(Box.createVerticalStrut(20));
        panel.add(btnSubmit);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnBack);

        return panel;
    }

    private JPanel createLockedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel lblLocked = new JLabel("COMPTE BLOQUÉ (5 minutes)", SwingConstants.CENTER);
        lblLocked.setFont(new Font("Arial", Font.BOLD, 24));
        lblLocked.setForeground(Color.RED);

        panel.add(lblLocked, BorderLayout.CENTER);
        return panel;
    }

    private void handleLogin() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, password, email, role FROM users WHERE username = ?")) { // Correction ici

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                String email = rs.getString("email"); // Maintenant accessible sans erreur
                String role = rs.getString("role");

                if (BCrypt.checkpw(password, storedHash)) {
                    onSuccessCallback.run();
                    dispose();
                } else {
                    handleFailedLogin();
                }
            } else {
                handleFailedLogin();
            }
        } catch (SQLException ex) {
            showError("Erreur de base de données : " + ex.getMessage()); // Afficher le détail de l'erreur
        }
    }

    private void handleFailedLogin() {
        loginAttempts++;
        if (loginAttempts >= 3) {
            cardLayout.show(cards, "locked");
            startLockTimer();
        } else {
            showError("Identifiants incorrects. Tentatives restantes: " + (3 - loginAttempts));
            highlightInvalidField(txtUsername);
            highlightInvalidField(txtPassword);
        }
    }

    private void handleRegistration() {
        String username = txtNewUser.getText();
        String email = txtRegisterEmail.getText();
        String password = new String(txtRegisterPass.getPassword());

        if (!validateRegistrationFields(username, email, password)) return;

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, email) VALUES (?, ?, ?)")) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, email);
            stmt.executeUpdate();

            showInfo("Compte créé avec succès !");
            cardLayout.show(cards, "login");
            clearRegistrationFields();

        } catch (SQLIntegrityConstraintViolationException e) {
            showError(e.getMessage().contains("username") ?
                    "Nom d'utilisateur déjà existant" : "Email déjà enregistré");
        } catch (SQLException e) {
            showError("Erreur lors de la création du compte");
        }
    }

    private boolean validateRegistrationFields(String username, String email, String password) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Tous les champs doivent être remplis");
            return false;
        }

        if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showError("Format d'email invalide");
            return false;
        }

        if (password.length() < 8) {
            showError("Le mot de passe doit contenir au moins 8 caractères");
            return false;
        }

        return true;
    }

    private void handleSendCode() {
        String email = txtEmail.getText();

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT username FROM users WHERE email = ?")) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                generatedCode = generateRandomCode();
                showInfo("Code envoyé à " + email + "\nCode: " + generatedCode);
            } else {
                showError("Aucun compte associé à cet email");
            }
        } catch (SQLException e) {
            showError("Erreur de vérification email");
        }
    }

    private void handlePasswordReset() {
        if (generatedCode == null || !generatedCode.equals(txtVerificationCode.getText())) {
            showError("Code de vérification invalide");
            return;
        }

        String newPassword = new String(txtNewPassword.getPassword());
        if (newPassword.length() < 8) {
            showError("Le mot de passe doit contenir 8 caractères minimum");
            return;
        }

        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE users SET password = ? WHERE email = ?")) {

            stmt.setString(1, hashedPassword);
            stmt.setString(2, txtEmail.getText());

            if (stmt.executeUpdate() > 0) {
                showInfo("Mot de passe réinitialisé avec succès");
                cardLayout.show(cards, "login");
                resetPasswordFields();
            }
        } catch (SQLException e) {
            showError("Erreur de mise à jour du mot de passe");
        }
    }

    // Méthodes utilitaires
    private String generateRandomCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private void startLockTimer() {
        if (lockTimer != null && lockTimer.isRunning()) return;

        lockTimer = new Timer(300000, e -> {
            cardLayout.show(cards, "login");
            loginAttempts = 0;
        });
        lockTimer.setRepeats(false);
        lockTimer.start();
    }

    private void clearRegistrationFields() {
        txtNewUser.setText("");
        txtRegisterEmail.setText("");
        txtRegisterPass.setText("");
    }

    private void resetPasswordFields() {
        txtEmail.setText("");
        txtVerificationCode.setText("");
        txtNewPassword.setText("");
    }

    private void highlightInvalidField(JComponent field) {
        field.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        new Timer(2000, e -> {
            field.setBorder(BorderFactory.createLineBorder(MAIN_COLOR));
        }).start();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    // Méthodes de style
    private JTextField createTextFieldWithPlaceholder(String placeholder) {
        JTextField field = new JTextField();
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAIN_COLOR),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        field.setMaximumSize(new Dimension(400, 40));
        field.setFont(new Font("Arial", Font.PLAIN, 14));

        // Ajout de placeholder personnalisé
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
        return field;
    }

    private JPasswordField createPasswordFieldWithPlaceholder(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MAIN_COLOR),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        field.setMaximumSize(new Dimension(400, 40));
        field.setFont(new Font("Arial", Font.PLAIN, 14));

        // Ajout de placeholder personnalisé
        field.setEchoChar((char) 0);
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (new String(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                    field.setEchoChar('•');
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setEchoChar((char) 0);
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
        return field;
    }

    private void styleMainButton(JButton btn) {
        btn.setBackground(MAIN_COLOR);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JButton createLinkButton(String text) {
        JButton btn = new JButton(text);
        btn.setForeground(MAIN_COLOR);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("Arial", Font.PLAIN, 14));
        return btn;
    }

    public String getLoggedInUserRole() {
        return loggedInUserRole;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new LoginFrame(() -> new Main().setVisible(true)).setVisible(true));
    }
}