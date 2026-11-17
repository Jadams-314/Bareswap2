
package cart;

import java.time.LocalDateTime;

public class Reservation {

    public static final String ACTIVE = "active";
    public static final String EXPIRED = "expired";
    public static final String COMPLETED = "completed";

    private int reservationId;
    private int userId;
    private int itemId;
    private int quantity;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String status;

    // Create a new reservation
    public Reservation(int reservationId, int userId,
                       int itemId, int quantity) {

        if (reservationId <= 0 || userId <= 0 || itemId <= 0 || quantity <= 0) {
            throw new IllegalArgumentException("Invalid reservation information.");
        }

        this.reservationId = reservationId;
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;

        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusMinutes(10);

        this.status = ACTIVE;
    }

    // Check whether reservation has expired
    public boolean isExpired() {
        return status.equals(ACTIVE)
                && !LocalDateTime.now().isBefore(expiresAt);
    }

    public void expire() {
        if (isExpired()) {
            status = EXPIRED;
        }
    }

    // Complete reservation after checkout
    public void complete() {
        if (isExpired()) {
            expire();
            throw new IllegalStateException("Reservation has expired.");
        }

        if (!status.equals(ACTIVE)) {
            throw new IllegalStateException("Reservation is not active.");
        }

        status = COMPLETED;
    }

    // Getters
    public int getReservationId() {
        return reservationId;
    }

    public int getUserId() {
        return userId;
    }

    public int getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getStatus() {
        return status;
    }
}
