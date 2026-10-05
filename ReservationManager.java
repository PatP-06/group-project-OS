public class ReservationManager {
    private volatile boolean useSynchronization = true; 

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

        if (useSynchronization) {
            synchronized (this) {
                return processReservation(ticketId, clientId, workerId);
            }
        } else {
            return processReservation(ticketId, clientId, workerId);
        }
    }

    private boolean processReservation(int ticketId, String clientId, int workerId) {
        ServerLogger.log("Worker-" + workerId, "LOCK", "Entering critical section");

        boolean available = (resourceTable != null) && resourceTable.isAvailable(ticketId);

        randomDelay();


        if (available) {
            if (resourceTable != null) {
                resourceTable.setReserved(ticketId, clientId);
            }
            
            ServerLogger.log("Worker-" + workerId, "RESERVE", "SUCCESS: Reserved ticket " + ticketId + " by " + clientId);
            System.out.println("SUCCESS: Reserved ticket " + ticketId + " by " + clientId);
            
            ServerLogger.log("Worker-" + workerId, "UNLOCK", "Leaving critical section");
            return true;
        } else {
            ServerLogger.log("Worker-" + workerId, "RESERVE", "FAILED: Ticket " + ticketId + " already reserved");
            System.out.println("FAILED: Ticket " + ticketId + " already reserved");
            
            ServerLogger.log("Worker-" + workerId, "UNLOCK", "Leaving critical section");
            return false;
        }
    }

    private void randomDelay() {
        try {
            int delay = (int)(Math.random() * 450) + 50;
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void setSynchronization(boolean enable) {
        this.useSynchronization = enable;
    }

    // Cancel method VVV
    public boolean cancel(int ticketId, String clientId, int workerId) {
        ServerLogger.log("Worker-" + workerId, "CHECK", "Checking Cancel for Ticket " + ticketId);
        if (useSynchronization) {
            synchronized (this) { // <--- ล็อกห้อง Critical Section
                return processCancellation(ticketId, clientId, workerId);
            }
        } else {
            return processCancellation(ticketId, clientId, workerId);
        }
    }
    private boolean isOwnerMatch(String owner, String clientId) {
        if (owner == null || clientId == null || owner.isEmpty() || clientId.isEmpty()) {
            return false;
        }
        String o = owner.trim().toUpperCase();
        String c = clientId.trim().toUpperCase();
        if (o.equals(c)) return true;                      
        if (o.startsWith(c + "-") || c.startsWith(o + "-")) return true; 
        if (c.equals("CLIENT-" + o) || o.equals("CLIENT-" + c)) return true;
        return false;
    }
    private boolean processCancellation(int ticketId, String clientId, int workerId) {
        ServerLogger.log("Worker-" + workerId, "LOCK", "Entering critical section (Cancel)");
        
        String owner = (resourceTable != null) ? resourceTable.getOwner(ticketId) : "";
        boolean isOwner = isOwnerMatch(owner, clientId);
        
        randomDelay();
        
        if (isOwner) {
            if (resourceTable != null) {
                resourceTable.setReserved(ticketId, ""); 
            }
            ServerLogger.log("Worker-" + workerId, "CANCEL", "SUCCESS: Cancelled ticket " + ticketId + " by " + clientId);
            ServerLogger.log("Worker-" + workerId, "UNLOCK", "Leaving critical section (Cancel)");
            return true;
        } else {
            ServerLogger.log("Worker-" + workerId, "CANCEL", "FAILED: You are not the owner for ticket " + ticketId);
            ServerLogger.log("Worker-" + workerId, "UNLOCK", "Leaving critical section (Cancel)");
            return false;
        }
    }
}
    