
package cart;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShoppingCart {

    private final Map<Integer, CartItem> items = new LinkedHashMap<>();

    // Add an item to the cart
    public void addItem(CartItem item) {

        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null.");
        }

        int itemId = item.getItemId();

        if (items.containsKey(itemId)) {
            CartItem existingItem = items.get(itemId);

            existingItem.setQuantity(
                existingItem.getQuantity() + item.getQuantity()
            );
        } else {
            items.put(itemId, item);
        }
    }

    // Remove an item from the cart
    public void removeItem(int itemId) {
        items.remove(itemId);
    }

    // Update an item's quantity
    public void updateQuantity(int itemId, int quantity) {

        CartItem item = items.get(itemId);

        if (item == null) {
            throw new IllegalArgumentException("Item not found.");
        }

        item.setQuantity(quantity);
    }

    // Get all cart items
    public Collection<CartItem> getItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    // Calculate the cart total
    public BigDecimal getTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : items.values()) {
            total = total.add(item.getSubtotal());
        }

        return total;
    }

    // Check whether the cart is empty
    public boolean isEmpty() {
        return items.isEmpty();
    }

    // Empty the cart
    public void clearCart() {
        items.clear();
    }
}
