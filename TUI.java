import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text UI helpers: colors, boxes, menu, seat grid, and result screens.
 * Contains no networking logic.
 */
public class TUI {

    private static boolean useColor = true;
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final int WIDTH = 60;

    private TUI() {}

    // ---------- settings ----------

    public static void setUseColor(boolean value) {
        useColor = value;
    }

    // ---------- low-level helpers ----------

    private static String c(String color, String text) {
        return useColor ? color + text + RESET : text;
    }

    private static String repeat(char ch, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(ch);
        return sb.toString();
    }

    private static String pad(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        return text + repeat(' ', width - text.length());
    }

    private static void boxTop(String title) {
        String t = " " + title + " ";
        System.out.println(c(CYAN, "+-" + t + repeat('-', WIDTH - t.length() - 1) + "+"));
    }

    private static void boxLine(String text, String color) {
        String body = pad(text, WIDTH - 2);
        System.out.println(c(CYAN, "|") + " " + (color == null ? body : c(color, body)) + " " + c(CYAN, "|"));
    }

    private static void boxLineRaw(String colored, int plainLen) {
        System.out.println(c(CYAN, "|") + " " + colored
                + repeat(' ', Math.max(0, WIDTH - 2 - plainLen)) + " " + c(CYAN, "|"));
    }

    private static void boxBottom() {
        System.out.println(c(CYAN, "+" + repeat('-', WIDTH) + "+"));
    }

    // ---------- screen control ----------

    public static void clear() {
        System.out.print("\033c");
    }

    // ---------- screens ----------

    public static void printUsage() {
        System.out.println("Usage: java Client <Client_Number> [--no-color]");
        System.out.println("Examples:");
        System.out.println("   java Client 1             (Interactive mode as Client-1)");
        System.out.println("   java Client kmutt         (Interactive mode as Client-kmutt)");
    }

    public static void printBanner(String clientId, String serverIp, int port) {
        System.out.println();
        boxTop("RESOURCE RESERVATION SYSTEM");
        boxLine("Logged in as: " + clientId, BOLD);
        boxLine("Server: " + serverIp + ":" + port, null);
        boxBottom();
    }

    public static void printConnected() {
        System.out.println(c(GREEN, " Connected to server successfully"));
    }

    public static void printConnectFailed(int port) {
        System.out.println("Connecting to Server Unsuccess [ PORT:" + port + " ]");
    }

    public static void printGoodbye() {
        System.out.println(c(CYAN, "\n Goodbye!"));
    }

    public static void printMenu() {
        System.out.println();
        boxTop("MENU");
        boxLine("[1] LIST      - show all resources", null);
        boxLine("[2] STATUS    - check one resource", null);
        boxLine("[3] RESERVE   - reserve a resource", null);
        boxLine("[4] CANCEL    - cancel your reservation", null);
        boxLine("[5] SHOOT     - stress test (5 clients each Resource)", null);
        boxLine("[0] QUIT", null);
        boxLine("You can also type a command, e.g.  RESERVE 10", YELLOW);
        boxBottom();
    }

    public static void printResult(String title, String sent, List<String> lines) {
        System.out.println();
        boxTop(title);
        boxLine("> " + sent, YELLOW);
        if (lines.isEmpty()) {
            boxLine("(no response from server)", RED);
        }
        for (String l : lines) {
            String upper = l.toUpperCase();
            String color = null;
            if (upper.contains("SUCCESS")) color = GREEN;
            else if (upper.contains("ERROR") || upper.contains("FAIL")) color = RED;
            boxLine(l, color);
        }
        boxBottom();
    }

    public static void printSeatGrid(String sent, List<String> lines, String clientId) {
        Pattern p = Pattern.compile("Ticket\\s+(\\d+)\\s*:\\s*([^\\]]*)", Pattern.CASE_INSENSITIVE);
        List<Integer> ids = new ArrayList<>();
        List<String> states = new ArrayList<>();
        for (String l : lines) {
            Matcher m = p.matcher(l);
            if (m.find()) {
                ids.add(Integer.parseInt(m.group(1)));
                states.add(m.group(2).trim());
            }
        }
        if (ids.isEmpty()) {
            printResult("LIST", sent, lines);
            return;
        }

        int free = 0, mine = 0, taken = 0;
        final int PER_ROW = 10;

        System.out.println();
        boxTop("TICKETS");
        for (int start = 0; start < ids.size(); start += PER_ROW) {
            StringBuilder colored = new StringBuilder();
            int plainLen = 0;
            for (int i = start; i < Math.min(start + PER_ROW, ids.size()); i++) {
                String num = String.format("%2d", ids.get(i));
                String state = states.get(i);
                String cell;
                if (state.equalsIgnoreCase("AVAILABLE")) {
                    cell = c(GREEN, "[" + num + "]");
                } else if (state.toUpperCase().contains(clientId.toUpperCase())) {
                    cell = c(YELLOW, "{" + num + "}");
                } else {
                    cell = c(RED, "(" + num + ")");
                }
                if (state.equalsIgnoreCase("AVAILABLE")) free++;
                else if (state.toUpperCase().contains(clientId.toUpperCase())) mine++;
                else taken++;

                if (i > start) { colored.append(' '); plainLen++; }
                colored.append(cell);
                plainLen += 4;
            }
            boxLineRaw(colored.toString(), plainLen);
        }
        boxLine("", null);
        boxLineRaw(c(GREEN, "[ 1] free") + "   " + c(RED, "( 1) taken") + "   " + c(YELLOW, "{ 1} yours"), 35);
        boxLine("Free: " + free + "   Taken: " + taken + "   Yours: " + mine, BOLD);
        boxBottom();
    }

    public static void printStressStart() {
        System.out.println(c(YELLOW, "\n Start stress test, wait a moment..."));
    }

    public static void printStressResult(int total, int success, int fail, long durationMs) {
        clear();
        System.out.println();
        boxTop("STRESS TEST RESULTS");
        boxLine("Total Requests Sent : " + total, null);
        boxLine("Successful Bookings : " + success, GREEN);
        boxLine("Rejected Bookings   : " + fail, RED);
        boxLine("Execution Time      : " + durationMs + " ms", null);
        boxBottom();
    }

    public static void printError(String message) {
        System.out.println(c(RED, " ! " + message));
    }

    // ---------- input ----------

    public static String ask(Scanner scanner, String prompt) {
        System.out.print(c(BOLD, " " + prompt + ": "));
        return scanner.nextLine().trim();
    }

    public static String promptCommand(Scanner scanner, String clientId) {
        System.out.print(c(BOLD, "\n " + clientId + " > "));
        return scanner.nextLine().trim();
    }
}
