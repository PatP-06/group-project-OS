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

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {

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

        try (
            Socket socket = new Socket(SERVER_IP, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            out.println(clientId);
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

                switch (command) {
                    case "LIST": {
                        socket.setSoTimeout(15000);
                        String cmd = "LIST";
                        out.println(cmd);
                        TUI.clear();
                        TUI.printSeatGrid(cmd, readServerResponse(in), clientId);
                        break;
                    }
                    case "STATUS": {
                        String cmd = "STATUS " + cmdArg;
                        out.println(cmd);
                        TUI.clear();
                        TUI.printResult("STATUS", cmd, readServerResponse(in));
                        break;
                    }
                    case "RESERVE": {
                        String cmd = "RESERVE " + cmdArg + " " + clientId;
                        out.println(cmd);
                        TUI.clear();
                        TUI.printResult("RESERVE", cmd, readServerResponse(in));
                        break;
                    }
                    case "CANCEL": {
                        String cmd = "CANCEL " + cmdArg + " " + clientId;
                        out.println(cmd);
                        TUI.clear();
                        TUI.printResult("CANCEL", cmd, readServerResponse(in));
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
        } catch (IOException e) {
            TUI.printConnectFailed(PORT);
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

        for (int i = 1; i <= TOTAL; i++) {
            final String client = "client-" + ((i - 1) % 5 + 1);
            final int currentResourceId = (i - 1) / 5 + 1;
            executor.submit(() -> {
                try (
                    Socket s = new Socket(SERVER_IP, PORT);
                    PrintWriter o = new PrintWriter(s.getOutputStream(), true);
                    BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()))
                ) {
                    ready.countDown();
                    startSignal.await();
                    o.println(client);
                    o.println("RESERVE " + currentResourceId + " " + client);
                    String resp = r.readLine();
                    if (resp != null && resp.contains("SUCCESS:")) successCount.incrementAndGet();
                    else failCount.incrementAndGet();
                } catch (Exception e) {
                    ready.countDown();
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

    public static List<String> readServerResponse(BufferedReader in) throws IOException {
        List<String> lines = new ArrayList<>();
        String line = in.readLine();
        if (line != null) {
            lines.add(line);
            while (in.ready()) {
                lines.add(in.readLine());
            }
        }
        return lines;
    }
}