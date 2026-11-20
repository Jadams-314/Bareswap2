import javax.swing.*;
import java.awt.*;
import java.sql.*;

//Loads SQLite JDBC driver and starts the UI
public class Main extends JFrame {


    private static final String DB_URL = "jdbc:sqlite:shop.db";

    private JTextField userField;
    private JPasswordField passField;
    private JLabel statusLabel;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    private String loggedInUsername = null;

    //Initializes database(used to be separate java file)
    //sets up the layout
    public Main() {
        setupDatabase();
        setTitle("Bare Swap - Portal");
        setSize(400, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createDashboardPanel(), "DASHBOARD");

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
    }

    //Creates the users and store tables(when/if they dont exists)
    //once the tables are created its essentially deprecated
    //'try' -> 'catch'
    private void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); 
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS store (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT, price REAL NOT NULL, seller_id TEXT NOT NULL, image_path TEXT)");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Setup Error: " + e.getMessage());
        }
    }
    //Uses GridBagLayout to create the login form UI
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Bare Swap Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Username:"), gbc);

        userField = new JTextField(15);
        gbc.gridx = 1;
        panel.add(userField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);

        passField = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        JButton loginButton = new JButton("Log In / Register");
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridy = 4;
        panel.add(statusLabel, gbc);

        loginButton.addActionListener(e -> handleLogin());

        return panel;
    }
    //Displays the navigation buttons for buying or selling
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel welcomeLabel = new JLabel("What would you like to do?", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JButton buyerButton = new JButton("Open Store (Buy Items)");
        JButton sellerButton = new JButton("Manage Listings (Sell Items)");

        buyerButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                new BareSwapBuyer(loggedInUsername).createAndShowGUI();
            });
});


        sellerButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                new BareSwapSeller(loggedInUsername).createAndShowGUI();
            });
        });

        panel.add(welcomeLabel);
        panel.add(buyerButton);
        panel.add(sellerButton);

        return panel;
    }
    //Validates fields(not empty)
    //Triggers authentication then switches to dashboard
    private void handleLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Fields cannot be empty.");
            return;
        }

        if (authenticateOrRegister(username, password)) {
            statusLabel.setText(" ");
            cardLayout.show(mainPanel, "DASHBOARD");
        }
    }
    //verifiees existing credentials
    //OR adds new user record into SQL db
    private boolean authenticateOrRegister(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getString("password").equals(password)) {
                        this.loggedInUsername = username;
                        return true;
                    } else {
                        statusLabel.setText("Incorrect password.");
                        return false;
                    }
                } else {
                    // Auto-register new user just like BareSwapSeller did
                    try (PreparedStatement ins = conn.prepareStatement("INSERT INTO users VALUES (?, ?)")) {
                        ins.setString(1, username);
                        ins.setString(2, password);
                        ins.executeUpdate();
                        this.loggedInUsername = username;
                        JOptionPane.showMessageDialog(this, "New account registered!");
                        return true;
                    }
                }
            }
        } catch (SQLException ex) {
            statusLabel.setText("DB Error: " + ex.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found.");
        }

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
