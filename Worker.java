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

                if(request.getCommand().equals("RESERVE")) {
                    manager.reserve(request.getTicketId(), request.getClientId(), workerId);
                }
                else if(request.getCommand().equals("CANCEL")) {
                    cancelManager.cancel(request.getTicketId(), request.getClientId(), workerId);
                }

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}