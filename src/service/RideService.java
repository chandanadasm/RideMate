package service;

import exception.InvalidRideException;
import exception.RideNotFoundException;
import interfaces.RideOperations;
import model.Ride;
import model.RideRequest;
import model.Student;
import supabase.SupabaseClient;
import supabase.SupabaseConfig;
import supabase.SupabaseRideRepository;
import supabase.SupabaseRideRequestRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Service managing ride operations and passenger ride requests.
 *
 * OOP Concepts Demonstrated:
 * 1. Interface Implementation & Polymorphism: Implements the RideOperations interface contract.
 * 2. Method Overriding: Overrides methods defined in RideOperations (@Override).
 * 3. Method Overloading: Two searchRide() methods with differing parameter lists.
 * 4. Collections Framework: Uses ArrayList<Ride> and ArrayList<RideRequest> to dynamically store and manage objects in memory.
 * 5. Exception Handling: Employs custom checked exceptions (InvalidRideException, RideNotFoundException).
 */
public class RideService implements RideOperations {

    // Collections Framework: Dynamic lists for storing active Ride and RideRequest objects
    private ArrayList<Ride> rides;
    private ArrayList<RideRequest> rideRequests;

    // Supabase Repository & Integration
    private SupabaseClient supabaseClient;
    private SupabaseRideRepository rideRepo;
    private SupabaseRideRequestRepository requestRepo;

    // Constructor initializing the in-memory collections and repositories
    public RideService() {
        this.rides = new ArrayList<>();
        this.rideRequests = new ArrayList<>();
        this.supabaseClient = new SupabaseClient();
        this.rideRepo = new SupabaseRideRepository(supabaseClient);
        this.requestRepo = new SupabaseRideRequestRepository(supabaseClient);
    }

    /**
     * Validates ride details prior to adding to the system.
     *
     * OOP Concept: Exception Handling (throws InvalidRideException on invalid data)
     */
    private void validateRide(Ride ride) throws InvalidRideException {
        if (ride == null) {
            throw new InvalidRideException("Ride cannot be null.");
        }
        if (ride.getRideId() == null || ride.getRideId().trim().isEmpty()) {
            throw new InvalidRideException("Ride ID cannot be empty.");
        }
        if (ride.getStartingLocation() == null || ride.getStartingLocation().trim().isEmpty()) {
            throw new InvalidRideException("Starting location cannot be empty.");
        }
        if (ride.getDestination() == null || ride.getDestination().trim().isEmpty()) {
            throw new InvalidRideException("Destination cannot be empty.");
        }
        if (ride.getAvailableSeats() <= 0) {
            throw new InvalidRideException("Available seats must be greater than 0.");
        }
        if (ride.getCostPerPerson() < 0) {
            throw new InvalidRideException("Cost per person cannot be negative.");
        }

        // Duplicate Check: Prevent posting two rides with the identical ID
        for (Ride existingRide : rides) {
            if (existingRide.getRideId() != null && existingRide.getRideId().equalsIgnoreCase(ride.getRideId().trim())) {
                throw new InvalidRideException("A ride with ID '" + ride.getRideId() + "' already exists.");
            }
        }
    }

    /**
     * Posts a new ride into the system.
     *
     * OOP Concept: Method Overriding (from RideOperations interface)
     * OOP Concept: Exception Handling (catches InvalidRideException to provide friendly feedback)
     * OOP Concept: Collections (adds element to ArrayList)
     */
    @Override
    public void offerRide(Ride ride) {
        try {
            validateRide(ride);
            // Collections Framework: add() appends the ride to the ArrayList
            rides.add(ride);
            // Persist to Supabase if configured
            if (supabase.SupabaseConfig.isConfigured()) {
                rideRepo.saveRide(ride);
            }
            System.out.println("Ride offered successfully! (Ride ID: " + ride.getRideId() + ")");
        } catch (InvalidRideException e) {
            // Exception Handling: Catching custom checked exception
            System.out.println("Failed to offer ride: " + e.getMessage());
        }
    }

    /**
     * Searches for rides matching source and destination.
     *
     * OOP Concept: Method Overloading (Signature 1: 2 parameters - source and destination)
     * OOP Concept: Method Overriding (implements RideOperations contract)
     * OOP Concept: Collections (iterates through ArrayList<Ride>)
     */
    @Override
    public void searchRide(String source, String destination) {
        if (source == null || destination == null || source.trim().isEmpty() || destination.trim().isEmpty()) {
            System.out.println("Search failed: Source and destination must be provided.");
            return;
        }

        System.out.println("\n--- Search Results for Route: " + source + " -> " + destination + " ---");
        boolean found = false;

        // Collections Framework: Enhanced for-loop traversing ArrayList<Ride>
        for (Ride ride : rides) {
            if (ride.getStartingLocation() != null && ride.getStartingLocation().equalsIgnoreCase(source.trim())
                    && ride.getDestination() != null && ride.getDestination().equalsIgnoreCase(destination.trim())) {
                printRideSummary(ride);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No rides found for route from '" + source + "' to '" + destination + "'.");
        }
    }

    /**
     * Searches for rides matching source, destination, and date.
     *
     * OOP Concept: Method Overloading (Signature 2: 3 parameters - source, destination, and date)
     * OOP Concept: Method Overriding (implements RideOperations contract)
     * OOP Concept: Collections (iterates through ArrayList<Ride>)
     */
    @Override
    public void searchRide(String source, String destination, String date) {
        if (source == null || destination == null || date == null
                || source.trim().isEmpty() || destination.trim().isEmpty() || date.trim().isEmpty()) {
            System.out.println("Search failed: Source, destination, and date must all be provided.");
            return;
        }

        System.out.println("\n--- Search Results for Route: " + source + " -> " + destination + " on " + date + " ---");
        boolean found = false;

        // Collections Framework: Enhanced for-loop traversing ArrayList<Ride>
        for (Ride ride : rides) {
            if (ride.getStartingLocation() != null && ride.getStartingLocation().equalsIgnoreCase(source.trim())
                    && ride.getDestination() != null && ride.getDestination().equalsIgnoreCase(destination.trim())
                    && ride.getDate() != null && ride.getDate().equalsIgnoreCase(date.trim())) {
                printRideSummary(ride);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No rides found from '" + source + "' to '" + destination + "' on " + date + ".");
        }
    }

    /**
     * Displays all available rides in the system.
     *
     * OOP Concept: Method Overriding (implements RideOperations contract)
     * OOP Concept: Collections (checks size and iterates through ArrayList)
     */
    @Override
    public void displayAllRides() {
        System.out.println("\n--- All Available Rides ---");
        if (rides.isEmpty()) {
            System.out.println("No rides are currently available.");
            return;
        }

        // Collections Framework: Loop through all items in ArrayList
        for (Ride ride : rides) {
            printRideSummary(ride);
        }
    }

    /**
     * Finds a ride by its unique ride ID.
     *
     * OOP Concept: Exception Handling (throws custom checked RideNotFoundException)
     * OOP Concept: Collections (linear search over ArrayList<Ride>)
     *
     * @param rideId The ID of the ride to find.
     * @return The found Ride object.
     * @throws RideNotFoundException if no ride matches the given ID.
     */
    public Ride findRideById(String rideId) throws RideNotFoundException {
        if (rideId == null || rideId.trim().isEmpty()) {
            throw new RideNotFoundException("Search failed: Ride ID cannot be empty.");
        }

        // Collections Framework: Linear scan through the ArrayList
        for (Ride ride : rides) {
            if (ride.getRideId() != null && ride.getRideId().equalsIgnoreCase(rideId.trim())) {
                return ride;
            }
        }

        // Exception Handling: Throw checked exception when element is absent
        throw new RideNotFoundException("Ride with ID '" + rideId + "' not found.");
    }

    /**
     * Books a seat on a ride.
     *
     * OOP Concept: Exception Handling (propagates RideNotFoundException, throws InvalidRideException)
     * OOP Concept: Encapsulation (updates availableSeats via setter)
     *
     * @param rideId The ID of the ride to book.
     * @return true if booking succeeded.
     * @throws RideNotFoundException if the ride does not exist.
     * @throws InvalidRideException  if no seats are available.
     */
    public boolean bookRide(String rideId) throws RideNotFoundException, InvalidRideException {
        // Find the ride (throws RideNotFoundException if missing)
        Ride ride = findRideById(rideId);

        // Exception Handling: Validate seat availability
        if (ride.getAvailableSeats() <= 0) {
            throw new InvalidRideException("Booking failed: No seats available for Ride ID: " + rideId);
        }

        // Encapsulation: Deduct seat through setter method
        ride.setAvailableSeats(ride.getAvailableSeats() - 1);
        if (supabase.SupabaseConfig.isConfigured()) {
            rideRepo.updateAvailableSeats(rideId, ride.getAvailableSeats());
        }
        System.out.println("Seat booked successfully on Ride ID: " + rideId + ". Seats remaining: " + ride.getAvailableSeats());
        return true;
    }

    /**
     * Helper method to print formatted ride information to the console.
     */
    private void printRideSummary(Ride ride) {
        String driverName = (ride.getDriver() != null) ? ride.getDriver().getName() : "N/A";
        System.out.println("----------------------------------------");
        System.out.println("Ride ID   : " + ride.getRideId());
        System.out.println("Driver    : " + driverName);
        System.out.println("Route     : " + ride.getStartingLocation() + " -> " + ride.getDestination());
        System.out.println("Date/Time : " + ride.getDate() + " at " + ride.getDepartureTime());
        System.out.println("Seats Left: " + ride.getAvailableSeats());
        System.out.println("Cost      : $" + String.format("%.2f", ride.getCostPerPerson()) + " per person");
    }

    // Search helper returning ArrayList for Swing UI table binding
    public ArrayList<Ride> findRides(String source, String destination) {
        ArrayList<Ride> results = new ArrayList<>();
        if (source == null || destination == null) return results;

        for (Ride ride : rides) {
            if (ride.getStartingLocation() != null && ride.getStartingLocation().equalsIgnoreCase(source.trim())
                    && ride.getDestination() != null && ride.getDestination().equalsIgnoreCase(destination.trim())) {
                results.add(ride);
            }
        }
        return results;
    }

    public ArrayList<Ride> findRides(String source, String destination, String date) {
        ArrayList<Ride> results = new ArrayList<>();
        if (source == null || destination == null || date == null) return results;

        for (Ride ride : rides) {
            if (ride.getStartingLocation() != null && ride.getStartingLocation().equalsIgnoreCase(source.trim())
                    && ride.getDestination() != null && ride.getDestination().equalsIgnoreCase(destination.trim())
                    && ride.getDate() != null && ride.getDate().equalsIgnoreCase(date.trim())) {
                results.add(ride);
            }
        }
        return results;
    }

    public ArrayList<Ride> getRidesByDriver(String studentId) {
        ArrayList<Ride> results = new ArrayList<>();
        if (studentId == null) return results;

        for (Ride ride : rides) {
            if (ride.getDriver() != null && studentId.equalsIgnoreCase(ride.getDriver().getStudentId())) {
                results.add(ride);
            }
        }
        return results;
    }

    public supabase.SupabaseRideRepository getRideRepository() {
        return rideRepo;
    }

    public SupabaseRideRequestRepository getRequestRepository() {
        return requestRepo;
    }

    /**
     * Synchronizes and refreshes the rides collection from Supabase.
     */
    public ArrayList<Ride> syncRidesFromSupabase() {
        if (SupabaseConfig.isConfigured()) {
            ArrayList<Ride> remoteRides = rideRepo.loadAllRides();
            if (remoteRides != null && !remoteRides.isEmpty()) {
                this.rides = remoteRides;
            }
        }
        return this.rides;
    }

    /**
     * Synchronizes and refreshes requests for the active student from Supabase.
     */
    public void syncRequestsFromSupabase(Student currentStudent) {
        if (currentStudent == null || !SupabaseConfig.isConfigured()) return;

        String userUuid = currentStudent.getId();
        String regNum = currentStudent.getStudentId();

        // Collect ride IDs offered by this student
        ArrayList<String> driverRideIds = new ArrayList<>();
        for (Ride r : rides) {
            if (r.getDriver() != null) {
                if ((userUuid != null && userUuid.equalsIgnoreCase(r.getDriver().getId()))
                        || (regNum != null && regNum.equalsIgnoreCase(r.getDriver().getStudentId()))) {
                    driverRideIds.add(r.getRideId());
                }
            }
        }

        // Fetch requests directed to this driver
        ArrayList<RideRequest> driverRequests = requestRepo.getRequestsForDriver(userUuid, driverRideIds);
        if (driverRequests != null) {
            for (RideRequest req : driverRequests) {
                mergeRequest(req);
            }
        }

        // Fetch requests created by this student
        if (userUuid != null && !userUuid.trim().isEmpty()) {
            ArrayList<RideRequest> myRequests = requestRepo.getRequestsByRequester(userUuid);
            if (myRequests != null) {
                for (RideRequest req : myRequests) {
                    mergeRequest(req);
                }
            }
        }
    }

    private void mergeRequest(RideRequest req) {
        if (req == null) return;
        for (int i = 0; i < rideRequests.size(); i++) {
            RideRequest existing = rideRequests.get(i);
            if (existing.getRequestId() != null && existing.getRequestId().equals(req.getRequestId())) {
                existing.setStatus(req.getStatus());
                if (req.getPickupLocation() != null && !req.getPickupLocation().isEmpty()) {
                    existing.setPickupLocation(req.getPickupLocation());
                }
                if (req.getDropoffLocation() != null && !req.getDropoffLocation().isEmpty()) {
                    existing.setDropoffLocation(req.getDropoffLocation());
                }
                return;
            }
        }
        // Enrich metadata if missing
        if (req.getRideRoute() == null || req.getRideRoute().isEmpty()) {
            try {
                Ride r = findRideById(req.getRideId());
                req.setRideRoute(r.getStartingLocation() + " -> " + r.getDestination());
                req.setRideDate(r.getDate() + " at " + r.getDepartureTime());
                if (req.getDropoffLocation() == null || req.getDropoffLocation().isEmpty()) {
                    req.setDropoffLocation(r.getDestination());
                }
            } catch (Exception ignored) {}
        }
        rideRequests.add(req);
    }

    /**
     * Submits a reservation request for a specific ride with default pickup location.
     */
    @Override
    public boolean requestRide(String rideId, Student requester, int seats) throws InvalidRideException, RideNotFoundException {
        return requestRide(rideId, requester, seats, null);
    }

    /**
     * Submits a reservation request for a specific ride including a custom pickup location.
     *
     * OOP Concept: Method Overloading
     * OOP Concept: Method Overriding (RideOperations contract)
     * OOP Concept: Exception Handling (Validates ride existence, available capacity, and self-booking)
     * OOP Concept: Collections Framework (Maintains dynamic list of requests in memory)
     */
    @Override
    public boolean requestRide(String rideId, Student requester, int seats, String pickupLocation) throws InvalidRideException, RideNotFoundException {
        if (rideId == null || rideId.trim().isEmpty()) {
            throw new RideNotFoundException("Ride ID cannot be empty.");
        }
        if (requester == null) {
            throw new InvalidRideException("Requester cannot be null.");
        }
        if (seats <= 0) {
            throw new InvalidRideException("Requested seats must be greater than 0.");
        }

        // Find the target ride (throws RideNotFoundException if missing)
        Ride ride = findRideById(rideId);

        // Validate seat availability
        if (ride.getAvailableSeats() <= 0) {
            throw new InvalidRideException("No seats available for Ride ID: " + rideId);
        }
        if (seats > ride.getAvailableSeats()) {
            throw new InvalidRideException("Requested seats (" + seats + ") exceeds available seats (" + ride.getAvailableSeats() + ").");
        }

        // Prevent drivers from requesting their own offered rides
        if (ride.getDriver() != null) {
            boolean sameUuid = requester.getId() != null && !requester.getId().trim().isEmpty()
                    && ride.getDriver().getId() != null
                    && requester.getId().trim().equalsIgnoreCase(ride.getDriver().getId().trim());
            boolean sameReg = requester.getStudentId() != null
                    && ride.getDriver().getStudentId() != null
                    && requester.getStudentId().trim().equalsIgnoreCase(ride.getDriver().getStudentId().trim());

            if (sameUuid || sameReg) {
                throw new InvalidRideException("You cannot request your own ride.");
            }
        }

        // Prevent duplicate pending requests for the same ride by the same student
        for (RideRequest existing : rideRequests) {
            if (existing.getRideId() != null && existing.getRideId().equalsIgnoreCase(rideId.trim()) && existing.isPending()) {
                boolean duplicate = false;
                if (requester.getId() != null && existing.getRequesterId() != null
                        && requester.getId().equalsIgnoreCase(existing.getRequesterId())) {
                    duplicate = true;
                } else if (requester.getStudentId() != null && existing.getRequester() != null
                        && requester.getStudentId().equalsIgnoreCase(existing.getRequester().getStudentId())) {
                    duplicate = true;
                }
                if (duplicate) {
                    throw new InvalidRideException("You already have an active pending request for Ride ID: " + rideId);
                }
            }
        }

        // Determine pickup and drop-off locations
        String effectivePickup = (pickupLocation != null && !pickupLocation.trim().isEmpty())
                ? pickupLocation.trim()
                : ride.getStartingLocation();
        String effectiveDropoff = ride.getDestination();

        // Generate unique UUID for the request
        String requestId = UUID.randomUUID().toString();
        String driverId = "";
        if (ride.getDriver() != null) {
            if (ride.getDriver().getId() != null && !ride.getDriver().getId().trim().isEmpty()) {
                driverId = ride.getDriver().getId().trim();
            } else {
                driverId = ride.getDriver().getStudentId();
            }
        }

        RideRequest request = new RideRequest(requestId, ride.getRideId(), requester, driverId, seats,
                effectivePickup, effectiveDropoff, "PENDING");
        request.setRideRoute(ride.getStartingLocation() + " -> " + ride.getDestination());
        request.setRideDate(ride.getDate() + " at " + ride.getDepartureTime());
        request.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // Collections Framework: Add to dynamic in-memory list
        rideRequests.add(request);

        // Supabase REST / RLS persistence
        if (SupabaseConfig.isConfigured()) {
            requestRepo.createRequest(request);
        }

        System.out.println("Ride request submitted successfully! (Request ID: " + requestId + ")");
        return true;
    }

    /**
     * Cancels an offered ride if requested by its driver.
     */
    @Override
    public boolean cancelRide(String rideId, String driverId) throws InvalidRideException, RideNotFoundException {
        if (rideId == null || rideId.trim().isEmpty()) {
            throw new RideNotFoundException("Ride ID cannot be empty.");
        }

        Ride ride = findRideById(rideId);
        if (driverId != null && !driverId.trim().isEmpty()) {
            boolean isOwner = false;
            if (ride.getDriver() != null) {
                if (driverId.equalsIgnoreCase(ride.getDriver().getId())
                        || driverId.equalsIgnoreCase(ride.getDriver().getStudentId())) {
                    isOwner = true;
                }
            }
            if (!isOwner) {
                throw new InvalidRideException("Unauthorized: You can only cancel or remove your own offered rides.");
            }
        }

        rides.remove(ride);

        if (SupabaseConfig.isConfigured()) {
            rideRepo.cancelRide(rideId, driverId);
        }

        System.out.println("Ride " + rideId + " cancelled successfully.");
        return true;
    }

    /**
     * Atomically accepts a pending ride request and deducts available seats.
     *
     * OOP Concept: Method Overriding (RideOperations contract)
     * OOP Concept: Encapsulation (Mutates ride state through setters)
     */
    @Override
    public boolean acceptRequest(String requestId) throws InvalidRideException, RideNotFoundException {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new InvalidRideException("Request ID cannot be empty.");
        }

        RideRequest req = findRequestById(requestId);
        if (!req.isPending()) {
            throw new InvalidRideException("Cannot accept: Request is already " + req.getStatus());
        }

        Ride ride = findRideById(req.getRideId());
        if (ride.getAvailableSeats() < req.getSeatsRequested()) {
            throw new InvalidRideException("Cannot accept request: Insufficient seats remaining ("
                    + ride.getAvailableSeats() + " available, " + req.getSeatsRequested() + " requested).");
        }

        // Encapsulation: Deduct seats and update status
        ride.setAvailableSeats(ride.getAvailableSeats() - req.getSeatsRequested());
        req.setStatus("ACCEPTED");

        // Supabase persistence: Executes atomic accept_ride_request RPC or REST PATCH
        if (SupabaseConfig.isConfigured()) {
            requestRepo.acceptRequest(requestId);
            rideRepo.updateAvailableSeats(ride.getRideId(), ride.getAvailableSeats());
        }

        System.out.println("Ride request " + requestId + " accepted! Seats remaining: " + ride.getAvailableSeats());
        return true;
    }

    /**
     * Declines a pending ride request.
     *
     * OOP Concept: Method Overriding (RideOperations contract)
     */
    @Override
    public boolean declineRequest(String requestId) throws InvalidRideException, RideNotFoundException {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new InvalidRideException("Request ID cannot be empty.");
        }

        RideRequest req = findRequestById(requestId);
        if (!req.isPending()) {
            throw new InvalidRideException("Cannot decline: Request is already " + req.getStatus());
        }

        req.setStatus("DECLINED");

        if (SupabaseConfig.isConfigured()) {
            requestRepo.declineRequest(requestId);
        }

        System.out.println("Ride request " + requestId + " declined.");
        return true;
    }

    /**
     * Finds a ride request by its unique UUID.
     */
    public RideRequest findRequestById(String requestId) throws RideNotFoundException {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new RideNotFoundException("Search failed: Request ID cannot be empty.");
        }

        for (RideRequest req : rideRequests) {
            if (req.getRequestId() != null && req.getRequestId().equalsIgnoreCase(requestId.trim())) {
                return req;
            }
        }
        throw new RideNotFoundException("Ride request with ID '" + requestId + "' not found.");
    }

    /**
     * Returns all requests received for rides offered by a specific driver.
     */
    public ArrayList<RideRequest> getRequestsForDriver(String driverIdOrReg) {
        ArrayList<RideRequest> results = new ArrayList<>();
        if (driverIdOrReg == null || driverIdOrReg.trim().isEmpty()) return results;

        for (RideRequest req : rideRequests) {
            boolean matches = false;
            if (req.getDriverId() != null && req.getDriverId().equalsIgnoreCase(driverIdOrReg.trim())) {
                matches = true;
            } else {
                try {
                    Ride r = findRideById(req.getRideId());
                    if (r.getDriver() != null) {
                        if (r.getDriver().getId() != null && r.getDriver().getId().equalsIgnoreCase(driverIdOrReg.trim())) {
                            matches = true;
                        } else if (r.getDriver().getStudentId() != null && r.getDriver().getStudentId().equalsIgnoreCase(driverIdOrReg.trim())) {
                            matches = true;
                        }
                    }
                } catch (RideNotFoundException ignored) {}
            }

            if (matches) {
                results.add(req);
            }
        }
        return results;
    }

    /**
     * Returns all requests submitted by a specific student requester.
     */
    public ArrayList<RideRequest> getRequestsByRequester(String requesterIdOrReg) {
        ArrayList<RideRequest> results = new ArrayList<>();
        if (requesterIdOrReg == null || requesterIdOrReg.trim().isEmpty()) return results;

        for (RideRequest req : rideRequests) {
            boolean matches = false;
            if (req.getRequesterId() != null && req.getRequesterId().equalsIgnoreCase(requesterIdOrReg.trim())) {
                matches = true;
            } else if (req.getRequester() != null && req.getRequester().getStudentId() != null
                    && req.getRequester().getStudentId().equalsIgnoreCase(requesterIdOrReg.trim())) {
                matches = true;
            }
            if (matches) {
                results.add(req);
            }
        }
        return results;
    }

    // Getters and Setters
    public ArrayList<Ride> getRides() {
        return rides;
    }

    public void setRides(ArrayList<Ride> rides) {
        this.rides = rides;
    }

    public ArrayList<RideRequest> getRideRequests() {
        return rideRequests;
    }

    public void setRideRequests(ArrayList<RideRequest> rideRequests) {
        this.rideRequests = rideRequests;
    }
}
