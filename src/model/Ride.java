package model;

/**
 * OOP Concept: Encapsulation & Aggregation
 * Represents a ride offered by a student driver.
 */
public class Ride {

    // Encapsulation: Private fields
    private String rideId;
    private Driver driver;
    private String startingLocation;
    private String destination;
    private String date;
    private String departureTime;
    private int availableSeats;
    private double costPerPerson;
    private String notes;

    // Default constructor
    public Ride() {
    }

    // Parameterized constructor
    public Ride(String rideId, Driver driver, String startingLocation, String destination,
                String date, String departureTime, int availableSeats, double costPerPerson) {
        this(rideId, driver, startingLocation, destination, date, departureTime, availableSeats, costPerPerson, "");
    }

    // Parameterized constructor with optional notes
    public Ride(String rideId, Driver driver, String startingLocation, String destination,
                String date, String departureTime, int availableSeats, double costPerPerson, String notes) {
        this.rideId = rideId;
        this.driver = driver;
        this.startingLocation = startingLocation;
        this.destination = destination;
        this.date = date;
        this.departureTime = departureTime;
        this.availableSeats = availableSeats;
        this.costPerPerson = costPerPerson;
        this.notes = (notes != null) ? notes.trim() : "";
    }

    // Displays details of this ride
    public void displayRideDetails() {
        String driverName = (driver != null) ? driver.getName() : "N/A";
        System.out.println("Ride [" + rideId + "] Driver: " + driverName + " | Route: " + startingLocation
                + " -> " + destination + " | Date: " + date + " " + departureTime
                + " | Seats Available: " + availableSeats + " | Fare: $" + String.format("%.2f", costPerPerson));
    }

    // Checks and reserves an available seat on this ride
    public boolean bookSeat() {
        if (hasAvailableSeat()) {
            this.availableSeats--;
            return true;
        }
        return false;
    }

    // Checks whether seats are currently available
    public boolean hasAvailableSeat() {
        return this.availableSeats > 0;
    }

    // Getters and Setters
    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public String getStartingLocation() {
        return startingLocation;
    }

    public void setStartingLocation(String startingLocation) {
        this.startingLocation = startingLocation;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public double getCostPerPerson() {
        return costPerPerson;
    }

    public void setCostPerPerson(double costPerPerson) {
        this.costPerPerson = costPerPerson;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
