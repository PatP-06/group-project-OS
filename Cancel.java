import java.util.concurrent.locks.ReentrantLock;

public class Cancel {
    private final ReentrantLock lock = new ReentrantLock();
    private SeatManager seatManager;

    public Cancel(SeatManager seatManager) {
        this.seatManager = seatManager;
    }

    public boolean cancelTicket(int ticketId, String clientId, int workerId){
        ServerLogger.log("Worker-" + workerId, "CHECK", "Checking cancel for Ticket " + ticketId);

        lock.lock();
        try {
            ServerLogger.log("Worker-" + workerId, "LOCK", "Entering critical section (Cancel)");

            if(ticketId < 1 || ticketId > 50) {
                ServerLogger.log("Worker-" + workerId, "CANCEL", "FAILED: Invalid ticket number");
                System.out.println("FAILED: Invalid ticket number");
                return false;
            }
            Seat targetSeat = seatManager.seats[ticketId - 1];

            boolean isReserved = targetSeat.getStatus().equals("RESERVE");
            boolean isOwner = targetSeat.getOwner().equals(clientId);

            if (isReserved && isOwner) {
                targetSeat.status = "AVAILABLE";
                targetSeat.owner = "";

                ServerLogger.log("Worker-" + workerId, "CANCEL", "SUCCESS: Ticket " + ticketId + " canceled by " + clientId);
                System.out.println("SUCCESS: Cancelled ticket " + ticketId);
                return true;

            } else {
                ServerLogger.log("Worker-" + workerId, "CANCEL", "FAILED: Ticket " + ticketId + " is not reserved by " + clientId);
                System.out.println("FAILED: You are not the owner");
                return false;
            }
        } finally {
            ServerLogger.log("Worker-" + workerId, "UNLOCK", "Leaving critical section");
            lock.unlock();
        }
    }
}
