
package cart;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ReservationService {

    private final Map<Integer, Reservation> reservations = new HashMap<>();
    private final Map<Integer, Integer> inventory = new HashMap<>();

    private final AtomicInteger nextReservationId = new AtomicInteger(1);

    // Register available inventory for an item
    public synchronized void registerItem(int itemId, int quantity) {

        if (itemId <= 0 || quantity < 0) {
            throw new IllegalArgumentException("Invalid inventory information.");
        }

        inventory.put(itemId, quantity);
    }

    // Create a temporary reservation
    public synchronized Reservation reserveItem(
            int userId, int itemId, int quantity) {

        expireReservations();

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        int available = inventory.getOrDefault(itemId, 0);

        if (quantity > available) {
            throw new IllegalStateException("Not enough inventory available.");
        }

        inventory.put(itemId, available - quantity);

        int reservationId = nextReservationId.getAndIncrement();

        Reservation reservation = new Reservation(
                reservationId, userId, itemId, quantity
        );

        reservations.put(reservationId, reservation);

        return reservation;
    }

    // Release inventory when a reservation expires
    public synchronized void expireReservations() {

        for (Reservation reservation : reservations.values()) {

            if (reservation.isExpired()) {

                reservation.expire();

                int itemId = reservation.getItemId();
                int quantity = reservation.getQuantity();

                int available = inventory.getOrDefault(itemId, 0);

                inventory.put(itemId, available + quantity);
            }
        }
    }

    // Complete a reservation after successful checkout
    public synchronized void completeReservation(int reservationId) {

        expireReservations();

        Reservation reservation = reservations.get(reservationId);

        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found.");
        }

        reservation.complete();
    }

    // Check available inventory
    public synchronized int getAvailableQuantity(int itemId) {

        expireReservations();

        return inventory.getOrDefault(itemId, 0);
    }

    // Retrieve a reservation
    public synchronized Reservation getReservation(int reservationId) {
        return reservations.get(reservationId);
    }
}
