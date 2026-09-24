import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
public class MessageQueue {
    private static BlockingQueue<Request> queue = new LinkedBlockingQueue<>();

    public static void addRequest(Request request) throws InterruptedException {
        queue.put(request);
    }

    public static Request getRequest() throws InterruptedException {
        return queue.take();
    }
}
