import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.*;

public class BareSwapSeller {
    private static final String DB_URL = "jdbc:sqlite:shop.db";
    private static String currentUser = "";

    private JFrame frame;
    private JTable itemTable;
    private DefaultTableModel tableModel;

    public static void main(String[] args) {
        setupDatabase();
        SwingUtilities.invokeLater(() -> {
            if (showLoginDialog()) {
                new BareSwapSeller().createAndShowGUI();
            }
        });
    }

    public BareSwapSeller() {
        
        tableModel = new DefaultTableModel(new String[]{"ID", "Image", "Name", "Description", "Price", "Seller"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 1) return ImageIcon.class;
                return super.getColumnClass(column);
            }
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private void createAndShowGUI() {
        frame = new JFrame("Secure Marketplace with Images - Logged in as: " + currentUser);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLocationRelativeTo(null);

        itemTable = new JTable(tableModel);
        itemTable.setRowHeight(60); 
        JScrollPane scrollPane = new JScrollPane(itemTable);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnRefresh = new JButton("Refresh Inventory");
        JButton btnSell = new JButton("Sell New Item");
        JButton btnEdit = new JButton("Edit Selected");
        JButton btnDelist = new JButton("Delist Selected");

        controlPanel.add(btnRefresh);
        controlPanel.add(btnSell);
        controlPanel.add(btnEdit);
        controlPanel.add(btnDelist);
        frame.add(controlPanel, BorderLayout.EAST);

        btnRefresh.addActionListener(e -> refreshTableData());
        btnSell.addActionListener(e -> showSellDialog());
        btnEdit.addActionListener(e -> handleEditAction());
        btnDelist.addActionListener(e -> handleDelistAction());

        refreshTableData();
        frame.setVisible(true);
    }

    private static void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS store (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT, price REAL NOT NULL, seller_id TEXT NOT NULL, image_path TEXT)");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage());
        }
    }

    private static boolean showLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        panel.add(new JLabel("Username:")); panel.add(userField);
        panel.add(new JLabel("Password:")); panel.add(passField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Marketplace Login / Register", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return false;

        String username = userField.getText().trim();
        String password = new String(passField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Fields cannot be empty.");
            return false;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getString("password").equals(password)) {
                        currentUser = username;
                        return true;
                    } else {
                        JOptionPane.showMessageDialog(null, "Incorrect password.");
                        return false;
                    }
                } else {
                    try (PreparedStatement ins = conn.prepareStatement("INSERT INTO users VALUES (?, ?)")) {
                        ins.setString(1, username); ins.setString(2, password);
                        ins.executeUpdate();
                        currentUser = username;
                        JOptionPane.showMessageDialog(null, "Account registered!");
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Auth Error: " + e.getMessage());
            return false;
        }
    }

    private void refreshTableData() {
        tableModel.setRowCount(0);
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM store")) {
            while (rs.next()) {
                String sellerStr = rs.getString("seller_id");
                if (sellerStr.equals(currentUser)) sellerStr += " (You)";

                
                String imgPath = rs.getString("image_path");
                ImageIcon thumbnail = null;
                if (imgPath != null && !imgPath.isEmpty()) {
                    File file = new File(imgPath);
                    if (file.exists()) {
                        ImageIcon rawIcon = new ImageIcon(imgPath);
                        
                        Image scaledImg = rawIcon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                        thumbnail = new ImageIcon(scaledImg);
                    }
                }

                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        thumbnail, 
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        sellerStr
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading items: " + e.getMessage());
        }
    }

    private void showSellDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField nameF = new JTextField();
        JTextField descF = new JTextField();
        JTextField priceF = new JTextField();
        JLabel imgLabel = new JLabel("No image chosen");
        JButton btnBrowse = new JButton("Choose Image File...");
        
        final StringBuilder selectedPath = new StringBuilder();

        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png"));
            int returnVal = chooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {

                selectedPath.setLength(0);
                selectedPath.append(chooser.getSelectedFile().getAbsolutePath());
                imgLabel.setText(chooser.getSelectedFile().getName());
            }
        });

        panel.add(new JLabel("Item Name:")); panel.add(nameF);
        panel.add(new JLabel("Description:")); panel.add(descF);
        panel.add(new JLabel("Price ($):")); panel.add(priceF);
        panel.add(btnBrowse); panel.add(imgLabel);

        int result = JOptionPane.showConfirmDialog(frame, panel, "List New Item", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO store (name, description, price, seller_id, image_path) VALUES (?, ?, ?, ?, ?)")) {
            pstmt.setString(1, nameF.getText().trim());
            pstmt.setString(2, descF.getText().trim());
            pstmt.setDouble(3, Double.parseDouble(priceF.getText().trim()));
            pstmt.setString(4, currentUser);
            pstmt.setString(5, selectedPath.toString());
            pstmt.executeUpdate();
            refreshTableData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error saving item. Check price format.");
        }
    }

    private int getSelectedItemId() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an item from the table first.");
            return -1;
        }
        return (int) tableModel.getValueAt(selectedRow, 0);
    }

    private boolean verifyOwnership(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT seller_id FROM store WHERE id = ?")) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getString("seller_id").equals(currentUser);
            }
        } catch (SQLException e) {
            return false;
        }
    }
    private void handleEditAction() {
    int id = getSelectedItemId();
    if (id == -1) return;

    if (!verifyOwnership(id)) {
        JOptionPane.showMessageDialog(frame, "You can only edit your own items.");
        return;
    }

    int selectedRow = itemTable.getSelectedRow();
    JTextField nameF = new JTextField(tableModel.getValueAt(selectedRow, 2).toString());
    JTextField descF = new JTextField(tableModel.getValueAt(selectedRow, 3).toString());
    JTextField priceF = new JTextField(tableModel.getValueAt(selectedRow, 4).toString());

    JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
    panel.add(new JLabel("Item Name:"));
    panel.add(nameF);
    panel.add(new JLabel("Description:"));
    panel.add(descF);
    panel.add(new JLabel("Price ($):"));
    panel.add(priceF);

    int result = JOptionPane.showConfirmDialog(frame, panel, "Edit Item", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION) return;

    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement pstmt = conn.prepareStatement("UPDATE store SET name = ?, description = ?, price = ? WHERE id = ?")) {
        pstmt.setString(1, nameF.getText().trim());
        pstmt.setString(2, descF.getText().trim());
        pstmt.setDouble(3, Double.parseDouble(priceF.getText().trim()));
        pstmt.setInt(4, id);
        pstmt.executeUpdate();
        refreshTableData();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(frame, "Error updating item. Check price format.");
    }
}

private void handleDelistAction() {
    int id = getSelectedItemId();
    if (id == -1) return;

    if (!verifyOwnership(id)) {
        JOptionPane.showMessageDialog(frame, "You can only delist your own items.");
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delist this item?", "Confirm Delist", JOptionPane.YES_NO_OPTION);
    if (confirm != JOptionPane.YES_OPTION) return;

    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement pstmt = conn.prepareStatement("DELETE FROM store WHERE id = ?")) {
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
        refreshTableData();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(frame, "Error delisting item: " + e.getMessage());
    }
}

}

