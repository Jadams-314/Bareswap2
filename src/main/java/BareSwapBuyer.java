import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.*;

public class BareSwapBuyer {
    private static final String DB_URL = "jdbc:sqlite:shop.db";

    private JFrame frame;
    private JTable itemTable;
    private DefaultTableModel tableModel;

    public static void main(String[] args) {
        setupDatabase();
        // Run UI safely on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> new BareSwapBuyer().createAndShowGUI());
    }

    public BareSwapBuyer() {
        // Construct table with an explicit Column Class for handling images natively
        tableModel = new DefaultTableModel(new String[]{"ID", "Image", "Item Name", "Description", "Price ($)", "Seller"}, 0) {
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
        frame = new JFrame("Marketplace Buyer Terminal");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 450);
        frame.setLocationRelativeTo(null);

        // Center Inventory Grid View
        itemTable = new JTable(tableModel);
        itemTable.setRowHeight(65); // Give breathing room for item images
        JScrollPane scrollPane = new JScrollPane(itemTable);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Right Sidebar Actions Menu
        JPanel sidebarPanel = new JPanel(new GridLayout(2, 1, 10, 15));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JButton btnRefresh = new JButton("Refresh Browser");
        JButton btnBuy = new JButton("🛒 Buy Selected");
        
        // Style the Buy Button visually
        btnBuy.setBackground(new Color(40, 167, 69));
        btnBuy.setForeground(Color.WHITE);
        btnBuy.setFocusPainted(false);
        btnBuy.setFont(new Font("Arial", Font.BOLD, 13));

        sidebarPanel.add(btnRefresh);
        sidebarPanel.add(btnBuy);
        frame.add(sidebarPanel, BorderLayout.EAST);

        // Operational Event Hooks
        btnRefresh.addActionListener(e -> refreshMarketData());
        btnBuy.addActionListener(e -> processPurchase());

        // Launch initial item scan
        refreshMarketData();
        frame.setVisible(true);
    }

    private static void setupDatabase() {
        // Initializes empty tables matching the SecureStore app schema if missing
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS store (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT, price REAL NOT NULL, seller_id TEXT NOT NULL, image_path TEXT)");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Connection Error: " + e.getMessage());
        }
    }

    private void refreshMarketData() {
        tableModel.setRowCount(0); // Wipe out old array cache
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM store")) {
            
            while (rs.next()) {
                String imgPath = rs.getString("image_path");
                ImageIcon thumbnail = null;

                // Safely search and display the image file
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
                        rs.getString("seller_id")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error pulling marketplace data: " + e.getMessage());
        }
    }

    private void processPurchase() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please choose an item from the list to purchase.");
            return;
        }

        // Target unique primary ID key from column 0
        int itemId = (int) tableModel.getValueAt(selectedRow, 0);
        String itemName = (String) tableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(frame, 
                "Are you sure you want to buy \"" + itemName + "\"?\nThis will remove it from the store inventory.", 
                "Confirm Purchase", JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;

        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM store WHERE id = ?")) {
            
            pstmt.setInt(1, itemId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(frame, "Purchase Complete! Item successfully acquired.");
                refreshMarketData(); 
            } else {
                JOptionPane.showMessageDialog(frame, "Transaction failed. The item may have already been sold.");
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Database write error: " + e.getMessage());
        }
    }
}
