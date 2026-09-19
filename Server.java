import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final int PORT = 8888;
    private static final int TOTAL_TICKETS = 50;

    private static final Map<Integer, String> ticketOwners = new ConcurrentHashMap<>();
    private static final AtomicInteger clientCounter = new AtomicInteger(1);

    // message queue vvv
    private static final BlockingQueue<BookingTask> messageQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        
    }
}