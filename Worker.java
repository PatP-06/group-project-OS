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

                //เคสงานตามคม.ที่ส่ง
                switch (request.getCommand().toUpperCase()) {
                    case "RESERVE":reservationManager.reserve(
                            request.getTicketId(),
                            request.getClientId(),
                            workerId
                        );
                        break;

                    /*case "CANCEL":
                        reservationManager.cancel(
                            request.getTicketId(),
                            request.getClientId(),
                            workerId
                        );
                        break;
                    */
                    default:
                        ServerLogger.log(
                            "Worker-" + workerId,
                            "ERROR",
                            "Unknown command: " + request.getCommand()
                        );
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