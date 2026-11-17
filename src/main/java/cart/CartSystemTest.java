
package cart;

public class CartSystemTest {

    public static void main(String[] args) {

        ReservationService service = new ReservationService();

        // Test 1: Register inventory
        service.registerItem(100, 5);

        System.out.println("Initial inventory: "
                + service.getAvailableQuantity(100));

        // Test 2: Reserve 2 items
        Reservation reservation =
                service.reserveItem(1, 100, 2);

        System.out.println("Reservation ID: "
                + reservation.getReservationId());

        // Test 3: Check remaining inventory
        System.out.println("Available after reservation: "
                + service.getAvailableQuantity(100));

        // Test 4: Complete checkout
        service.completeReservation(
                reservation.getReservationId()
        );

        System.out.println("Reservation status: "
                + reservation.getStatus());

        System.out.println("Available after checkout: "
                + service.getAvailableQuantity(100));

                

    }
}
