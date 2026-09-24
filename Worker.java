public class Worker implements Runnable {
    private int workerId;
    private ReservationManager manager;
    private Cancel cancelManager;

    public Worker(int workerId, ReservationManager manager, Cancel cancelManager) {
        this.workerId = workerId;
        this.manager = manager;
        this.cancelManager = cancelManager;
    }

    @Override
    public void run() {
        while(true) {
            try {
                Request request = MessageQueue.getRequest();
                ServerLogger.log("Worker-" + workerId, "RECEIVED", request.getCommand() + " " + request.getTicketId() + " from " + request.getClientId());

                if (request.getCommand().equals("RESERVE")) {
                    boolean success = manager.reserve(request.getTicketId(), request.getClientId(), workerId);
                    if (success) {
                        request.getResponse().complete(request.getTicketId());
                    } else {
                        request.getResponse().complete(request.getTicketId());
                    }
                } else if (request.getCommand().equals("CANCEL")) {
                    boolean success = cancelManager.cancelTicket(request.getTicketId(), request.getClientId(), workerId);
                    if (success) {
                        request.getResponse().complete(request.getTicketId());
                    } else {
                        request.getResponse().complete("FAILED: You are not the owner");
                    }
                }

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}