import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String sender, String action, String message){
        String timestamp = LocalDateTime.now().format(dtf);

        String logMessage = String.format("[%s] [%-10s] [%-8s] %s", timestamp, sender, action, message);

        // dtf stand for date-time-format
        // timestamp is LocalDateTime_format
        // sender is client
        // action is reserve-success-failed? cancel-success-failed? quit?
        // message is "STRING HERE" show on server-log

        // HOW TO USE:
        // ServerLogger.log(String sender, String action, String message);

        System.out.println(logMessage);

    }
}