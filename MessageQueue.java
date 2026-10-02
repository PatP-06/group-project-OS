import java.util.LinkedList;

public class MessageQueue {
    private static final LinkedList<Request> queue = new LinkedList<>();

    //ไคเอนส่งคำขอเข้านี่ เพิ่มรีเควดเข้าคิว ฮ่าฮ่า
    public static synchronized void addRequest(Request request) {
        queue.add(request);
        ServerLogger.log("QUEUE", "ADD", request.getClientId()+ " -> " + request.getCommand() + " " + request.getTicketId());

        MessageQueue.class.notifyAll();
    }

    public static synchronized Request getRequest() throws InterruptedException {
        while (queue.isEmpty()) {
            ServerLogger.log("QUEUE", "WAIT", "Worker waiting for request");
            MessageQueue.class.wait();
        }
        Request request = queue.removeFirst();
        ServerLogger.log("QUEUE", "FETCH", request.getClientId()+ " -> "+ request.getCommand()+ " "+ request.getTicketId());

        return request;
    }
}

