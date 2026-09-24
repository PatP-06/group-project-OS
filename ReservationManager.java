import java.util.concurrent.locks.ReentrantLock;

public class ReservationManager {
    private final ReentrantLock lock = new ReentrantLock();
    private boolean useSynchronization = true; // off/on Mutex for test Race Condition

    public interface ResourceTableBridge { 
        boolean isAvailable(int ticketId);
        void setReserved(int ticketId, String clientId);
        String getOwner(int ticketId);
    }

    private ResourceTableBridge resourceTable;

    public ReservationManager(boolean useSynchronization, ResourceTableBridge resourceTable) {
        this.useSynchronization = useSynchronization;
        this.resourceTable = resourceTable;
    }

    public boolean reserve(int ticketId, String clientId, int workerId) {
        ServerLogger.log("Worker-" + workerId, "CHECK", "Checking Ticket " + ticketId);

        if (useSynchronization) {lock.lock();}

        try {
            ServerLogger.log("Worker-" + workerId, "LOCK", "Entering critical section");
            boolean available = (resourceTable != null) && resourceTable.isAvailable(ticketId);

            randomDelay();

            if (available) {
                if (resourceTable != null) {
                    resourceTable.setReserved(ticketId, clientId);
                }
                
                ServerLogger.log("Worker-" + workerId, "RESERVE", "SUCCESS: Reserved ticket " + ticketId + " by " + clientId);
                System.out.println("SUCCESS: Reserved ticket " + ticketId);
                return true;
            } else {
                ServerLogger.log("Worker-" + workerId, "RESERVE", "FAILED: Ticket " + ticketId + " already reserved");
                System.out.println("FAILED: Ticket " + ticketId + " already reserved");
                return false;
            }

        } finally {
            ServerLogger.log("Worker-" + workerId, "UNLOCK", "Leaving critical section");
            if (useSynchronization) {
                lock.unlock();
            }
        }
    }

    private void randomDelay() {
        try {
            int delay = (int)(Math.random() * 450) + 50; // Delay 50 - 500 ms
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void setSynchronization(boolean enable) {
        this.useSynchronization = enable;
    }
}