import java.util.concurrent.CompletableFuture;

public class Request {
    private String command;
    private int ticketId;
    private String clientId;
    private CompletableFuture<String> response = new CompletableFuture<>();

    public Request(String command, int ticketId, String clientId) {
        this.command = command;
        this.ticketId = ticketId;
        this.clientId = clientId;
    }

    public String getCommand() {
        return command;
    }

    public int getTicketId() {
        return ticketId;
    }

    public String getClientId() {
        return clientId;
    }

    public CompletableFuture<String> getResponse() {
        return response;
    }
}