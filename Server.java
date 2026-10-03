
public class Server {
    public static void main(String[] args) {
        
        SeatManager seatManager = new SeatManager();

        //สร้าง Bridge เชื่อม SeatManager เข้ากับ ReservationManager
        ReservationManager.ResourceTableBridge bridge = new ReservationManager.ResourceTableBridge() {
            @Override
            public boolean isAvailable(int ticketId) {
                if (ticketId < 1 || ticketId > seatManager.seats.length) return false;
                return seatManager.seats[ticketId - 1].getStatus().equals("AVAILABLE"); // count from ticketId 1 to 50
            }

            @Override
            public void setReserved(int ticketId, String clientId) {
                if (ticketId >= 1 && ticketId <= seatManager.seats.length) {
                    if (clientId == null || clientId.isEmpty()) {
                        seatManager.seats[ticketId - 1].status = "AVAILABLE";
                        seatManager.seats[ticketId - 1].owner = "";
                    } else {
                        seatManager.seats[ticketId - 1].status = "RESERVE";
                        seatManager.seats[ticketId - 1].owner = clientId; // count from clientId
                    }
                }
            }

            @Override
            public String getOwner(int ticketId) {
                if (ticketId >= 1 && ticketId <= seatManager.seats.length) {
                    return seatManager.seats[ticketId - 1].getOwner();
                }
                return "";
            }
        };

        // ReservationManager
        ReservationManager reservationManager = new ReservationManager(true, bridge);

        for (int i = 1; i <= 3; i++) {
            Thread workerThread = new Thread(new Worker(i, reservationManager));
            workerThread.setDaemon(true);
            workerThread.start();
        }

        ServerLogger.log("SERVER", "START", "Concurrent Reservation Server started");
        System.out.println("==========================================================");
        System.out.println(" Server is READY!");
        System.out.println(" Available commands in Terminal:");
        System.out.println("==========================================================");


        System.out.println(" Server is running and listening on MessageQueue...");

        while (true) {
            Request request = MessageQueue.getIpcRequest();

            if (request != null) {
                String cmd = request.getCommand().toUpperCase();

                // LIST
                if (cmd.equals("LIST")) {
                    MessageQueue.sendResponse(request.getClientId(), seatManager.listSeat().trim());
                    ServerLogger.log("SERVER", "LIST", "Sent seat list to " + request.getClientId());
                } 
                // STATUS
                else if (cmd.equals("STATUS")) {
                    MessageQueue.sendResponse(request.getClientId(), seatManager.statusSeat(request.getTicketId()));
                    ServerLogger.log("SERVER", "STATUS", "Sent status of ticket " + request.getTicketId() + " to " + request.getClientId());
                } 
                // QUIT
                else if (cmd.equals("QUIT")) {
                    MessageQueue.sendResponse(request.getClientId(), "Thankyou for using");
                    ServerLogger.log(request.getClientId(), "QUIT", "Client finished session");
                } 
                // RESERVE หรือ CANCEL
                else {
                    MessageQueue.addRequest(request);
                }
            } else {
                try {
                    Thread.sleep(30); // พัก 30ms ถ้าคิวว่าง เพื่อไม่ให้กิน CPU ไม่มีก็ได้แต่มีก็จะเท่กว่า
                } catch (InterruptedException ignored) {}
            }
        }
    }
}