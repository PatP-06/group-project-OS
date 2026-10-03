import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MessageQueue {
    // IPC
    private static final File REQ_DIR = new File("ipc_queue/requests");
    private static final File RES_DIR = new File("ipc_queue/responses");
    private static final AtomicLong counter = new AtomicLong(0);

    private static final LinkedList<Request> workerQueue = new LinkedList<>();

    public static void init() {
        if (!REQ_DIR.exists()) REQ_DIR.mkdirs();
        if (!RES_DIR.exists()) RES_DIR.mkdirs();
    }

    // --- ฝั่ง Client: ส่งคำขอลงไฟล์ IPC ---
    public static void sendRequest(String clientId, String command, int ticketId) {
        init();
        long timestamp = System.currentTimeMillis();
        long seq = counter.incrementAndGet();

        String filename = String.format("%014d_%05d_%s.req", timestamp, seq, clientId);
        File file = new File(REQ_DIR, filename);

        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println(clientId);
            out.println(command);
            out.println(ticketId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- ฝั่ง Server: ดึงคำขอ IPC จาก Client ---
    public static Request getIpcRequest() {
        init();
        File[] files = REQ_DIR.listFiles((dir, name) -> name.endsWith(".req"));
        if (files == null || files.length == 0) return null;

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            File lockFile = new File(file.getAbsolutePath() + ".lock");
            
            if (file.renameTo(lockFile)) {
                try (BufferedReader in = new BufferedReader(new FileReader(lockFile))) {
                    String clientId = in.readLine();
                    String command = in.readLine();
                    int ticketId = Integer.parseInt(in.readLine());

                    lockFile.delete();
                    return new Request(command, ticketId, clientId);
                } catch (Exception e) {
                    lockFile.delete();
                }
            }
        }
        return null;
    }

    // --- ฝั่ง Server/Worker: ส่งผลลัพธ์กลับหา Client ---
    public static void sendResponse(String clientId, String responseMessage) {
        init();
        File tmp = new File(RES_DIR, clientId + ".res.tmp");
        File file = new File(RES_DIR, clientId + ".res");
        try (PrintWriter out = new PrintWriter(new FileWriter(tmp))) {
            out.println(responseMessage);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        tmp.renameTo(file);
    }

    public static String receiveResponse(String clientId) {
        return receiveResponse(clientId, 10000);
    }

    // --- ฝั่ง Client: รอรับผลลัพธ์จาก Server ---
    public static String receiveResponse(String clientId, long timeoutMs) {
        init();
        File file = new File(RES_DIR, clientId + ".res");
        long startTime = System.currentTimeMillis();

        while (!file.exists()) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                return "FAILED: Request timeout";
            }
            try {
                Thread.sleep(30);
            } catch (InterruptedException ignored) {}
        }

        String result;
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
            result = sb.toString();
        } catch (Exception e) {
            return "FAILED: Cannot read response";
        }
        file.delete();
        return result;
    }

    public static synchronized void addRequest(Request request) {
        workerQueue.add(request);
        ServerLogger.log("QUEUE", "ADD", request.getClientId() + " -> " + request.getCommand() + " " + request.getTicketId());
        
        MessageQueue.class.notifyAll();
    }

    public static synchronized Request getRequest() throws InterruptedException {
        while (workerQueue.isEmpty()) {
            ServerLogger.log("QUEUE", "WAIT", "Worker waiting for request");
            MessageQueue.class.wait(); 
        }
        
        Request request = workerQueue.removeFirst();
        ServerLogger.log("QUEUE", "FETCH", request.getClientId() + " -> " + request.getCommand() + " " + request.getTicketId());
        return request;
    }
}