public class SeatManager {
    Seat[] seats = new Seat[50];

    public SeatManager() {
        for (int i = 0; i < seats.length; i++) {
            seats[i] = new Seat(i + 1);
        }
    }

    public String listSeat() {
        String result = "";
        for (int j = 0; j < seats.length; j++) {
            Seat targetSeat = seats[j];
            if (seats[j].getStatus().contains("AVAILABLE")) {
                result += "[Ticket " + seats[j].getSeatNumber() + ": " + seats[j].getStatus() + "]\n";
            }else if (seats[j].getStatus().contains("RESERVE")) {
                result += "[Ticket " + seats[j].getSeatNumber() + ": " + targetSeat.getOwner() + "]\n";
            }
        }
        return result;
    }

    public String statusSeat(int id) {
        if (id < 1 || id > seats.length) {
            return "ERROR: that seat number doesn't exist";
        }
        Seat targetSeat = seats[id - 1];

        if (targetSeat.getStatus().equals("RESERVE")) {
            return "Ticket " + targetSeat.getSeatNumber() + " is RESERVE by " + targetSeat.getOwner();
        } else {
            return "Ticket " + targetSeat.getSeatNumber() + " is AVAILABLE";
        }
    }
}