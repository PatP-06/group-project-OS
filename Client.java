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
    import java.util.concurrent.*;
    import java.util.concurrent.atomic.AtomicInteger;
    import java.util.Scanner;

    public class Client {
        //set ip & port
        public static final String SERVER_IP = "127.0.0.1";
        public static final int PORT = 8080;

        public static void main(String[] args) throws InterruptedException {

            //warning error cmd argument
            if (args.length < 1)    {
                System.out.println("Usage: java Client <Client_Number>");
                System.out.println("Examples:");
                System.out.println("   java Client 1             (Interactive mode as Client-1)");
                System.out.println("   java Client kmutt         (Interactive mode as Client-kmutt)");
                return;
            };

            //set client
            final String clientId = "Client-" + args[0];
            Scanner scanner = new Scanner(System.in);
            String command = "0";
            String cmdArg = "0";
            //main send & read
            try (
                //set socket&Buffer
                Socket socket = new Socket(SERVER_IP, PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ){
                
                System.out.println(" Connecting to Server success");
                System.out.println(" Connected to Server as [" + clientId + "]");
                //check cmd argument
                while (!"QUIT".equalsIgnoreCase(command)){
                    boolean matched = false;
                    if(args.length == 1 ){
                        System.out.println("=====================================================================================================");
                        System.out.println(" Commands supported:");
                        System.out.println("    LIST                   - List all resources ( List )");
                        System.out.println("    STATUS <id>            - Check status of resource (e.g. STATUS 10 )");
                        System.out.println("    RESERVE <id>           - Reserve resource (e.g. RESERVE 10 )");
                        System.out.println("    CANCEL <id>            - Cancel reservation (e.g. CANCEL 10 )");
                        System.out.println("    QUIT                   - (e.g. QUIT)");
                        System.out.println(" Special command:");
                        System.out.println("    SHOOT                  - Shoot 1,500 client to test race condition (e.g. java client SHOOT 1)");
                        System.out.println("=====================================================================================================");
                    }
                    System.out.println(clientId + " >");
                    String line = scanner.nextLine();
                    String[] parts = line.trim().split("\\s+");
                    command = parts.length > 0 ? parts[0] : "";
                    cmdArg = parts.length > 1 ? parts[1] : "";


                    //"LIST" cas
                    if ("LIST".equalsIgnoreCase(command)) {
                        String cmd = "LIST";
                        System.out.println("[LIST] Sending: " + cmd);
                        out.println(cmd);
                        socket.setSoTimeout(15000);
                        readServerResponse(in);
                        matched = true;
                    }

                    //"STATUS" case
                    if ("STATUS".equalsIgnoreCase(command)) {
                        String cmd = "STATUS " + cmdArg;
                        System.out.println("[STATUS] Sending: " + cmd);
                        out.println(cmd);
                        readServerResponse(in);
                        matched = true;
                    }

                    //"RESERVE" case
                    if ("RESERVE".equalsIgnoreCase(command)) {
                        String cmd = "RESERVE " + cmdArg + " " + clientId;
                        System.out.println("[RESERVE] Sending: " + cmd);
                        out.println(cmd);
                        readServerResponse(in);
                        matched = true;
                    }

                    //"CANCEL" case
                    if ("CANCEL".equalsIgnoreCase(command)) {
                        String cmd = "CANCEL " + cmdArg + " " + clientId;
                        System.out.println("[CANCEL] Sending: " + cmd);
                        out.println(cmd);
                        readServerResponse(in);
                        matched = true;
                    }

                    if ("SHOOT".equalsIgnoreCase(command)) {
                        //set  threads wait & get ready to execute
                        int resourceId = 1;
                        final int TOTAL = 1500;
                        ExecutorService executor = Executors.newFixedThreadPool(1500);
                        CountDownLatch ready = new CountDownLatch(TOTAL);
                        CountDownLatch startSignal = new CountDownLatch(1);
                        CountDownLatch done = new CountDownLatch(TOTAL);
                        //set variable count success & fail
                        AtomicInteger successCount = new AtomicInteger(0);
                        AtomicInteger failCount = new AtomicInteger(0);
                        for (int i = 1; i <= TOTAL; i++){
                            final String id = "user-" + i;
                            final int currentResourceId = resourceId;
                            executor.submit(() -> {
                                try(
                                    Socket s = new Socket(SERVER_IP, PORT);
                                    PrintWriter o = new PrintWriter(s.getOutputStream(), true);
                                    BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()))
                                ){
                                    ready.countDown();
                                    startSignal.await();               // ยิงพร้อมกัน
                                    o.println("RESERVE " + Integer.toString(currentResourceId) + " " + id);
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

                            if( resourceId == 50){
                                resourceId = 1;
                            }else{
                                resourceId++;
                            }
                        }

                        //execute threads
                        ready.await();                                 // wait thread ready
                        long start = System.currentTimeMillis();
                        System.out.println("Start stress test, wait a moments");
                        startSignal.countDown();                       // execute simultaneously
                        done.await();
                        executor.shutdown();
                        long duration = System.currentTimeMillis() - start;
                        System.out.println("==========================================");
                        System.out.println("          STRESS TEST RESULTS             ");
                        System.out.println("==========================================");
                        System.out.println("Total Requests Sent : " + TOTAL);
                        System.out.println("Successful Bookings : " + successCount.get());
                        System.out.println("Rejected Bookings   : " + failCount.get());
                        System.out.println("Execution Time      : " + duration + " ms");
                        System.out.println("==========================================\n");
                        matched = true;
                    }

                    if ("QUIT".equalsIgnoreCase(command)) {
                        System.out.println("Goodbye!");
                        return;
                    }else if(!matched){
                        System.out.println(clientId + " > [" + command + "]: command not found");
                    }
                }

            }
            //if can't connect to server
            catch (IOException e){
                System.out.println("Connecting to Server Unsuccess [ PORT:" + PORT + " ]");
            }
            
            //if have problem with threads while stress test
            catch (InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while waiting for stress test");
            }
        }

        public static void readServerResponse(BufferedReader in) throws IOException {
            String line = in.readLine();
            if (line != null) {
                System.out.println("[Server]: " + line);
                while (in.ready()) {
                    System.out.println("[Server]: " + in.readLine());
                }
            }
        }
    }