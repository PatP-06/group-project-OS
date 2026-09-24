import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final int PORT = 8080;
    private static final AtomicInteger clientCounter = new AtomicInteger(1);

    public static void main(String[] args) {
        SeatManager seatManager = new SeatManager();

        ReservationManager.ResourceTableBridge bridge = new ReservationManager.ResourceTableBridge() {
            @Override
            public boolean isAvailable(int ticketId) {
                if (ticketId < 1 || ticketId > seatManager.seats.length) return false;
                return seatManager.seats[ticketId - 1].getStatus().equals("AVAILABLE");
            }

            @Override
            public void setReserved(int ticketId, String clientId) {
                if (ticketId >= 1 && ticketId <= seatManager.seats.length) {
                    seatManager.seats[ticketId - 1].status = "RESERVE";
                    seatManager.seats[ticketId - 1].owner = clientId;
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

        ReservationManager reservationManager = new ReservationManager(true, bridge);
        Cancel cancelManager = new Cancel(seatManager);

        for (int i = 1; i <= 3; i++) {
            Thread workerThread = new Thread(new Worker(i, reservationManager, cancelManager));
            workerThread.setDaemon(true);
            workerThread.start();
        }

        ServerLogger.log("SERVER", "START", "Ticket Server is running on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            ServerLogger.log("SERVER", "READY", "Waiting for client connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientId = "Client-" + clientCounter.getAndIncrement();
                new Thread(() -> handleClient(clientSocket, clientId, seatManager)).start();
            }
        } catch (IOException e) {
            ServerLogger.log("SERVER", "ERROR", "Server exception: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket, String clientId, SeatManager seatManager) {
        ServerLogger.log(clientId, "CONNECT", "Connected from " + socket.getRemoteSocketAddress());

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 0 || parts[0].isEmpty()) continue;

                String command = parts[0].toUpperCase();

                switch (command) {
                    case "LIST":
                        out.println(seatManager.listSeat().trim());
                        ServerLogger.log(clientId, "LIST", "Viewed all tickets");
                        break;

                    case "STATUS":
                        if (parts.length < 2) {
                            out.println("ERROR: Please specify ticket number (e.g. STATUS 10)");
                            break;
                        }
                        int sId = parseTicketId(parts[1]);
                        out.println(seatManager.statusSeat(sId));
                        ServerLogger.log(clientId, "STATUS", "Checked ticket " + sId);
                        break;

                    case "RESERVE":
                        if (parts.length < 2) {
                            out.println("ERROR: Please specify ticket number (e.g. RESERVE 10)");
                            break;
                        }
                        int rId = parseTicketId(parts[1]);
                        Request reserveReq = new Request("RESERVE", rId, clientId);
                        MessageQueue.addRequest(reserveReq);
                        String reserveRes = reserveReq.getResponse().join();
                        out.println(reserveRes);
                        break;

                    case "CANCEL":
                        if (parts.length < 2) {
                            out.println("ERROR: Please specify ticket number (e.g. CANCEL 10)");
                            break;
                        }
                        int cId = parseTicketId(parts[1]);
                        Request cancelReq = new Request("CANCEL", cId, clientId);
                        MessageQueue.addRequest(cancelReq);
                        String cancelRes = cancelReq.getResponse().join();
                        out.println(cancelRes);
                        break;

                    case "QUIT":
                        Quit.handleQuit(clientId, out, socket);
                        return;

                    default:
                        out.println("ERROR: Unknown command. Allowed: LIST, STATUS, RESERVE, CANCEL, QUIT");
                        break;
                }
            }
        } catch (Exception e) {
            ServerLogger.log(clientId, "ERROR", "Connection error: " + e.getMessage());
        } finally {
            Quit.handleQuit(clientId, null, socket);
        }
    }

    private static int parseTicketId(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return -1;
        }
    }
}