package interfaces;

import exception.InvalidRideException;
import exception.RideNotFoundException;
import model.Ride;
import model.Student;

/**
 * OOP Concept: Interface & Method Overloading
 * Contract defining operations for offering, searching, viewing, and managing ride requests.
 */
public interface RideOperations {

    // Post a new ride
    void offerRide(Ride ride);

    // OOP Concept: Method Overloading (Signature 1: Source and Destination)
    void searchRide(String source, String destination);

    // OOP Concept: Method Overloading (Signature 2: Source, Destination, and Travel Date)
    void searchRide(String source, String destination, String date);

    // Display all available rides
    void displayAllRides();

    // Ride request operations
    boolean requestRide(String rideId, Student requester, int seats) throws InvalidRideException, RideNotFoundException;
    // OOP Concept: Method Overloading (Signature with pickupLocation)
    boolean requestRide(String rideId, Student requester, int seats, String pickupLocation) throws InvalidRideException, RideNotFoundException;
    boolean acceptRequest(String requestId) throws InvalidRideException, RideNotFoundException;
    boolean declineRequest(String requestId) throws InvalidRideException, RideNotFoundException;
    boolean cancelRide(String rideId, String driverId) throws InvalidRideException, RideNotFoundException;
}
