import java.util.Scanner;

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


        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String firstWord = parts[0].toUpperCase();

            // LIST
            if (firstWord.equals("LIST")) {
                System.out.println(seatManager.listSeat().trim());
                ServerLogger.log("SERVER", "LIST", "Displayed seat list");
                continue;
            }

            // STATUS
            if (firstWord.equals("STATUS")) {
                if (parts.length < 2) {
                    System.out.println("ERROR: Please specify ticket number");
                    continue;
                }
                int tId = Integer.parseInt(parts[1]);
                System.out.println(seatManager.statusSeat(tId));
                ServerLogger.log("SERVER", "STATUS", "Checked ticket " + tId);
                continue;
            }

            // QUIT
            if (firstWord.equals("QUIT")) {
                System.out.println("Thankyou for using");
                ServerLogger.log("SERVER", "QUIT", "Server shutting down");
                break;
            }

            // CANCEL
            if (parts.length >= 3) {
                String clientId = parts[0];
                String command = parts[1].toUpperCase();
                int ticketId = Integer.parseInt(parts[2]);

                MessageQueue.addRequest(new Request(command, ticketId, clientId));
            } else {
                System.out.println("ERROR: Invalid format. Use: <ClientId> <Command> <TicketId> (e.g. Client-1 RESERVE 10)");
            }
        }
    }
}