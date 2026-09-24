public class Seat {
    int seatNumber;
    String status;
    String owner;

    public Seat(int number) {
        seatNumber = number;
        status = "AVAILABLE";
        owner = "";
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getOwner() {
        return owner;
    }
}
