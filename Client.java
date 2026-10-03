/*
    นะโม ตัสสะ ภะคะวะโต อะระหะโต สัมมาสัมพุทธัสสสะ (3 จบ)
    นโม พุทธายะ พระพุทธะไตรรัตนญาณ
    มณีนพรัตน์ สีสะหัสสะ สุธรรมา
    พุทโธ ธัมโม สังโฆ
    ยะ-ธา-พุท-โม-นะ
    พุทธะบูชา ธัมมะบูชา สังฆะบูชา
    อัคคีทานัง วะรังคันธัง
    สีวะลี จะ มะหาเถรัง
    อะหัง วันทามิ ทูระโต
    อะหัง วันทามิ ธาตุโย
    อะหัง วันทามิ สัพพะโส
    พุทธะ ธัมมะ สังฆะ ปูเชมิ
*/

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {

    // เก็บไว้เพราะ TUI.printBanner ยังใช้แสดงผล (ไม่ได้ใช้เชื่อมต่อจริงแล้ว)
        public static final String SERVER_IP = "127.0.0.1";
        public static final int PORT = 8080;

    public static void main(String[] args) throws InterruptedException {
        TUI.clear();

        if (args.length < 1) {
            TUI.printUsage();
            return;
        }
        for (String a : args) {
            if (a.equals("--no-color")) TUI.setUseColor(false);
        }

        final String clientId = "Client-" + args[0];
        Scanner scanner = new Scanner(System.in);

        MessageQueue.init();
        TUI.printBanner(clientId, SERVER_IP, PORT);
        TUI.printConnected();

        while (true) {
            TUI.printMenu();
            String line = TUI.promptCommand(scanner, clientId);
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String command = parts[0].toUpperCase();
            String cmdArg = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "1": command = "LIST"; break;
                case "2": command = "STATUS"; break;
                case "3": command = "RESERVE"; break;
                case "4": command = "CANCEL"; break;
                case "5": command = "SHOOT"; break;
                case "0": command = "QUIT"; break;
                default: break;
            }

            if ((command.equals("STATUS") || command.equals("RESERVE") || command.equals("CANCEL"))
                    && cmdArg.isEmpty()) {
                cmdArg = TUI.ask(scanner, "Resource id");
                if (cmdArg.isEmpty()) {
                    TUI.printError("Resource id is required");
                    continue;
                }
            }

            // MessageQueue.sendRequest รับ ticketId เป็น int
            int ticketId = 0;
            if (command.equals("STATUS") || command.equals("RESERVE") || command.equals("CANCEL")) {
                try {
                    ticketId = Integer.parseInt(cmdArg);
                } catch (NumberFormatException e) {
                    TUI.printError("Resource id must be a number");
                    continue;
                }
            }

            switch (command) {
                case "LIST": {
                    String cmd = "LIST";
                    MessageQueue.sendRequest(clientId, cmd, 0);
                    List<String> resp = readResponse(clientId);
                    TUI.clear();
                    TUI.printSeatGrid(cmd, resp, clientId);
                    break;
                }
                case "STATUS": {
                    String cmd = "STATUS " + cmdArg;
                    MessageQueue.sendRequest(clientId, "STATUS", ticketId);
                    List<String> resp = readResponse(clientId);
                    TUI.clear();
                    TUI.printResult("STATUS", cmd, resp);
                    break;
                }
                case "RESERVE": {
                    String cmd = "RESERVE " + cmdArg + " " + clientId;
                    MessageQueue.sendRequest(clientId, "RESERVE", ticketId);
                    List<String> resp = readResponse(clientId);
                    TUI.clear();
                    TUI.printResult("RESERVE", cmd, resp);
                    break;
                }
                case "CANCEL": {
                    String cmd = "CANCEL " + cmdArg + " " + clientId;
                    MessageQueue.sendRequest(clientId, "CANCEL", ticketId);
                    List<String> resp = readResponse(clientId);
                    TUI.clear();
                    TUI.printResult("CANCEL", cmd, resp);
                    break;
                }
                case "SHOOT":
                    runStressTest();
                    break;
                case "QUIT":
                    TUI.printGoodbye();
                    return;
                default:
                    TUI.clear();
                    TUI.printError("[" + parts[0] + "]: command not found");
            }
        }
    }

        private static void runStressTest() throws InterruptedException {
        final int TOTAL = 250;
        ExecutorService executor = Executors.newFixedThreadPool(TOTAL);
        CountDownLatch ready = new CountDownLatch(TOTAL);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(TOTAL);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 1. สร้างลิสต์คำขอทั้งหมด 250 รายการ (50 ตั๋ว x 5 clients)
        List<int[]> tasks = new ArrayList<>();
        for (int resId = 1; resId <= 50; resId++) {
            for (int clientNum = 1; clientNum <= 5; clientNum++) {
                tasks.add(new int[]{resId, clientNum});
            }
        }
        
        // 2. สลับลำดับคำขอแบบสุ่ม เพื่อให้ทุก Client มีโอกาสเข้าคิวก่อน-หลังเท่าเทียมกัน
        Collections.shuffle(tasks);

        for (int i = 0; i < TOTAL; i++) {
            final int[] task = tasks.get(i);
            final int currentResourceId = task[0];
            final int clientNum = task[1];
            final String client = "client-" + clientNum + "-" + (i + 1);

            executor.submit(() -> {
                try {
                    ready.countDown();
                    startSignal.await();

                    // 3. จำลอง Network Jitter (1-15 ms) เหมือนตอนใช้ Socket ให้เกิดการแข่งขัน Concurrency จริง
                    int jitter = ThreadLocalRandom.current().nextInt(1, 15);
                    Thread.sleep(jitter);

                    MessageQueue.sendRequest(client, "RESERVE", currentResourceId);
                    String resp = MessageQueue.receiveResponse(client, 120000);
                    if (resp != null && resp.contains("SUCCESS:")) successCount.incrementAndGet();
                    else failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        long start = System.currentTimeMillis();
        TUI.printStressStart();
        startSignal.countDown();
        done.await();
        executor.shutdown();
        long duration = System.currentTimeMillis() - start;

        TUI.printStressResult(TOTAL, successCount.get(), failCount.get(), duration);
    }

    // รอรับผลลัพธ์จาก MessageQueue แล้วห่อเป็น List<String> ให้เข้ากับ TUI เดิม
    public static List<String> readResponse(String clientId) {
        List<String> lines = new ArrayList<>();
        String resp = MessageQueue.receiveResponse(clientId);
        if (resp != null) {
            for (String l : resp.split("\n")) lines.add(l);
        }
        return lines;
    }
}
