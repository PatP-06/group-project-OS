import java.io.IOException;
import java.net.Socket;

public class Quit {
    public static void handleQuit(String clientId, PrintWriter writer, Socket socket) {
        if (writer != null) {
            writer.println("Thankyou for using");
        }
        ServerLogger.log(clientId, "QUIT", "Client requested to disconnect");
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            ServerLogger.log(clientId, "DISCONNECT", "Session closed gracefully");
        } catch (IOException e) {
            ServerLogger.log(clientId, "ERROR", "Error closing socket: " + e.getMessage());
        }
    }
}