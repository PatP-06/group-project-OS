import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MessageQueue {
    // mq ipc
    private static final File REQ_DIR = new File("ipc_queue/requests");
    private static final File RES_DIR = new File("ipc_queue/responses");
    private static final AtomicLong counter = new AtomicLong(0);

    // create ipc_queue folder
    public static void init() {
        if (!REQ_DIR.exists()) REQ_DIR.mkdirs();
        if (!RES_DIR.exists()) RES_DIR.mkdirs();
    }

    // ========================================================
    // ฝั่ง Client: ส่งคำขอลง Request Queue
    // ========================================================
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

    // ========================================================
    // ฝั่ง Server: ดึงคำขอจาก Request Queue (FIFO + Atomic Lock)
    // ========================================================
    public static Request getRequest() {
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

    // ========================================================
    // ฝั่ง Server: ส่งผลลัพธ์กลับไปยัง Response Queue ของ Client คนนั้น
    // ========================================================
    public static void sendResponse(String clientId, String responseMessage) {
        init();
        File file = new File(RES_DIR, clientId + ".res");
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println(responseMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ========================================================
    // ฝั่ง Client: รอรับผลลัพธ์จาก Response Queue
    // ========================================================
    public static String receiveResponse(String clientId) {
        init();
        File file = new File(RES_DIR, clientId + ".res");
        long startTime = System.currentTimeMillis();

        // ให้เวลารอไม่เกิน 10 วินาที
        while (!file.exists()) {
            if (System.currentTimeMillis() - startTime > 10000) {
                return "FAILED: Request timeout";
            }
            try {
                Thread.sleep(30); // รอ 30 ms ก่อนตรวจสอบอีกครั้ง เพื่อไม่ให้ CPU ทำงานหนัก
            } catch (InterruptedException ignored) {}
        }

        // อ่านผลลัพธ์จากไฟล์และลบไฟล์นั้นออก
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String result = in.readLine();
            file.delete();
            return result;
        } catch (Exception e) {
            return "FAILED: Cannot read response";
        }
    }
}