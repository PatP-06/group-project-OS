public class Worker implements Runnable {
    private final int workerId;
    private final ReservationManager reservationManager;

    public Worker(int workerId, ReservationManager reservationManager) {
        this.workerId = workerId;
        this.reservationManager = reservationManager;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Request request = MessageQueue.getRequest();
                ServerLogger.log("Worker-" + workerId, "RECEIVED", request.getCommand() + " " + request.getTicketId() + " from " + request.getClientId());

                switch (request.getCommand().toUpperCase()) {
                    case "RESERVE":
                        boolean reserved = reservationManager.reserve(
                            request.getTicketId(),
                            request.getClientId(),
                            workerId
                        );
                        if (reserved) {
                            MessageQueue.sendResponse(request.getClientId(), "SUCCESS: Reserved ticket " + request.getTicketId());
                        } else {
                            MessageQueue.sendResponse(request.getClientId(), "FAILED: Ticket " + request.getTicketId() + " already reserved");
                        }
                        break;

                    case "CANCEL":
                        boolean cancelled = reservationManager.cancel(
                            request.getTicketId(),
                            request.getClientId(),
                            workerId
                        );
                        if (cancelled) {
                            MessageQueue.sendResponse(request.getClientId(), "SUCCESS: Cancelled ticket " + request.getTicketId());
                        } else {
                            MessageQueue.sendResponse(request.getClientId(), "FAILED: Cannot cancel ticket " + request.getTicketId());
                        }
                        break;

                    default:
                        ServerLogger.log("Worker-" + workerId, "ERROR", "Unknown command: " + request.getCommand());
                        MessageQueue.sendResponse(request.getClientId(), "FAILED: Unknown command " + request.getCommand());
                        break;
                }

            } catch (InterruptedException e) {
                ServerLogger.log("Worker-" + workerId, "STOPPED", "Worker interrupted, shutting down.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}