public class Worker implements Runnable {
    private int workerId;
    private ReservationManager manager;

    public Worker(int workerId, ReservationManager manager) {
        this.workerId = workerId;
        this.manager = manager;
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

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}