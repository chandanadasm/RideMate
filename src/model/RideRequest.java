package model;

/**
 * OOP Concept: Encapsulation & Aggregation
 * Represents a passenger ride reservation request within the CampusRide system.
 * Maps to the 'public.ride_requests' table in Supabase.
 */
public class RideRequest {

    // Encapsulation: Private member variables
    private String requestId;      // UUID primary key (matches public.ride_requests.id)
    private String rideId;         // Foreign key to public.rides.id
    private Student requester;     // Aggregation: RideRequest has-a Student
    private String requesterId;    // UUID foreign key to public.students.id
    private String driverId;       // UUID foreign key to public.students.id
    private int seatsRequested;    // Number of passenger seats requested
    private String pickupLocation; // Specific passenger pickup point
    private String dropoffLocation;// Confirmed drop-off point (ride destination)
    private String status;         // PENDING, ACCEPTED, DECLINED, CANCELLED
    private String createdAt;      // Timestamp of request submission

    // Descriptive metadata for UI presentation
    private String rideRoute;      // e.g., "Palakkad -> NSSCE"
    private String rideDate;       // e.g., "2026-10-15 08:30 AM"

    // Default constructor
    public RideRequest() {
        this.status = "PENDING";
        this.seatsRequested = 1;
    }

    // Parameterized constructor
    public RideRequest(String requestId, String rideId, Student requester, String driverId, int seatsRequested, String status) {
        this(requestId, rideId, requester, driverId, seatsRequested, "", "", status);
    }

    // Parameterized constructor including pickup and dropoff locations
    public RideRequest(String requestId, String rideId, Student requester, String driverId, int seatsRequested,
                       String pickupLocation, String dropoffLocation, String status) {
        this.requestId = requestId;
        this.rideId = rideId;
        this.requester = requester;
        this.requesterId = (requester != null) ? requester.getId() : null;
        this.driverId = driverId;
        this.seatsRequested = (seatsRequested > 0) ? seatsRequested : 1;
        this.pickupLocation = (pickupLocation != null) ? pickupLocation.trim() : "";
        this.dropoffLocation = (dropoffLocation != null) ? dropoffLocation.trim() : "";
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "PENDING";
    }

    // Status evaluation helpers
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(this.status);
    }

    public boolean isAccepted() {
        return "ACCEPTED".equalsIgnoreCase(this.status);
    }

    public boolean isDeclined() {
        return "DECLINED".equalsIgnoreCase(this.status);
    }

    // Getters and Setters (Encapsulation)
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public Student getRequester() {
        return requester;
    }

    public void setRequester(Student requester) {
        this.requester = requester;
        if (requester != null && requester.getId() != null) {
            this.requesterId = requester.getId();
        }
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public int getSeatsRequested() {
        return seatsRequested;
    }

    public void setSeatsRequested(int seatsRequested) {
        this.seatsRequested = seatsRequested;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = (status != null) ? status.trim().toUpperCase() : "PENDING";
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getRideRoute() {
        return rideRoute;
    }

    public void setRideRoute(String rideRoute) {
        this.rideRoute = rideRoute;
    }

    public String getRideDate() {
        return rideDate;
    }

    public void setRideDate(String rideDate) {
        this.rideDate = rideDate;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDropoffLocation() {
        return dropoffLocation;
    }

    public void setDropoffLocation(String dropoffLocation) {
        this.dropoffLocation = dropoffLocation;
    }
}
