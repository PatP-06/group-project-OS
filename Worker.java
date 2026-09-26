public class Worker implements Runnable {

    private int workerId;
    private ReservationManager manager;
    private Cancel cancelManager;

    public Worker(int workerId,
                  ReservationManager manager,
                  Cancel cancelManager) {
        this.workerId = workerId;
        this.manager = manager;
        this.cancelManager = cancelManager;
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) { // Null exception in case bugged worker doesn't know their status as they carried the request or not ; what ifs error[catch] in error[null]

            Request request = null; 

            try {
              
                request = MessageQueue.getRequest();

                ServerLogger.log(
                    "Worker-" + workerId,
                    "RECEIVED",
                    request.getCommand()
                        + " "
                        + request.getTicketId()
                        + " from "
                        + request.getClientId()
                );

                if ("RESERVE".equals(request.getCommand())) { // Null exception in case bugged string == null (request.getCommand().equals("RESERVE")[old code] ; null = 'reserve' #unknown command)

                    boolean success = manager.reserve(
                        request.getTicketId(),
                        request.getClientId(),
                        workerId
                    );

                    if (success) {
                        request.getResponse()
                            .complete("SUCCESS: You have reserved ticket " + request.getTicketId());
                    } else {
                        request.getResponse()
                            .complete("FAILED: Reservation failed");
                    }

                } else if ("CANCEL".equals(request.getCommand())) {

                    boolean success = cancelManager.cancelTicket(
                        request.getTicketId(),
                        request.getClientId(),
                        workerId
                    );

                    if (success) {
                        request.getResponse()
                            .complete("SUCCESS: You have canceled ticket " + request.getTicketId());
                    } else {
                        request.getResponse()
                            .complete("FAILED: You are not the owner");
                    }

                } else {

                    request.getResponse()
                        .complete("FAILED: Unknown command");
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {

                e.printStackTrace();

                if (request != null) {
                    request.getResponse()
                        .complete("FAILED: Internal server error");
                }
            }
        }
    }
}