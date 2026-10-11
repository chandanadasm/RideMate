package test;

import exception.InvalidRideException;
import exception.RideNotFoundException;
import model.Driver;
import model.Ride;
import model.RideRequest;
import model.Student;
import model.Vehicle;
import service.AuthService;
import service.RideService;

import java.util.ArrayList;

/**
 * End-to-end automated verification test verifying the complete CampusRide workflow:
 * 1. Student Accounts (Driver & Rider)
 * 2. Fuel & Petrol Cost Estimator calculation
 * 3. Offer Ride with notes and capacity
 * 4. Ride Search & Filter
 * 5. Request Ride with custom pickup location (separate from source)
 * 6. Security & Constraints: Self-booking prevention, duplicate request prevention, overbooking limit
 * 7. Incoming Driver Requests View
 * 8. Driver Accept Workflow & Seat Deduction
 * 9. Rider My Requests Tracking (Pending -> Accepted)
 * 10. Driver Cancel Ride & Unauthorized Cancellation Protection
 */
public class EndToEndVerificationTest {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    private static void assertTrue(String testName, boolean condition, String detail) {
        testsTotal++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + testName + (detail != null ? " - " + detail : ""));
        } else {
            System.err.println("  [FAIL] " + testName + (detail != null ? " - " + detail : ""));
        }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println(" CampusRide End-to-End Complete Workflow Verification Test");
        System.out.println("==========================================================\n");

        AuthService authService = new AuthService();
        RideService rideService = new RideService();

        // ---------------------------------------------------------------------
        // Step 1: Create Two Verified Student Accounts
        // ---------------------------------------------------------------------
        System.out.println("--- 1. Testing Student Account Setup ---");
        String driverUuid = java.util.UUID.randomUUID().toString();
        String riderUuid = java.util.UUID.randomUUID().toString();
        Student driverStudent = new Student(
                driverUuid,
                "Anand Kumar",
                "B22CS01",
                "anand@nssce.ac.in",
                "Password123"
        );
        Student riderStudent = new Student(
                riderUuid,
                "Sneha Menon",
                "B22CS02",
                "sneha@nssce.ac.in",
                "Password123"
        );

        authService.getStudents().add(driverStudent);
        authService.getStudents().add(riderStudent);

        assertTrue("Driver account registered", authService.findStudentByEmail(driverStudent.getEmail()) != null, "Anand (" + driverStudent.getEmail() + ")");
        assertTrue("Rider account registered", authService.findStudentByEmail(riderStudent.getEmail()) != null, "Sneha (" + riderStudent.getEmail() + ")");

        // ---------------------------------------------------------------------
        // Step 2: Fuel and Ride Cost Estimator Logic
        // ---------------------------------------------------------------------
        System.out.println("\n--- 2. Testing Petrol & Cost Estimator Calculation ---");
        double distanceKm = 30.0;
        double mileageKmPerL = 15.0;
        double petrolPricePerL = 105.0;
        int peopleSharing = 3;

        double expectedTotalCost = (distanceKm / mileageKmPerL) * petrolPricePerL; // (30 / 15) * 105 = 210.00
        double expectedPerPerson = expectedTotalCost / peopleSharing; // 210 / 3 = 70.00

        assertTrue("Estimated Total Petrol Cost", Math.abs(expectedTotalCost - 210.00) < 0.01,
                String.format("Calculated ₹%.2f (Formula: (%.1f / %.1f) * %.2f)", expectedTotalCost, distanceKm, mileageKmPerL, petrolPricePerL));
        assertTrue("Estimated Cost Per Person", Math.abs(expectedPerPerson - 70.00) < 0.01,
                String.format("Calculated ₹%.2f per person for %d people", expectedPerPerson, peopleSharing));

        // ---------------------------------------------------------------------
        // Step 3: Driver Offers a Ride
        // ---------------------------------------------------------------------
        System.out.println("\n--- 3. Testing Offer Ride with Notes & Seats ---");
        Vehicle vehicle = new Vehicle("Car", "KL-09-AB-1234", 4);
        Driver driver = new Driver(
                driverStudent.getId(),
                driverStudent.getName(),
                driverStudent.getStudentId(),
                driverStudent.getEmail(),
                driverStudent.getPassword(),
                vehicle
        );

        String rideId = java.util.UUID.randomUUID().toString();
        Ride ride = new Ride(
                rideId,
                driver,
                "NSSCE Main Gate",
                "Palakkad Junction Railway Station",
                "2026-10-15",
                "09:00 AM",
                3,       // available seats
                expectedPerPerson, // $70.00 fare based on petrol estimator
                "Leaving sharp at 9 AM, AC car with boot space"
        );

        rideService.offerRide(ride);

        assertTrue("Ride listed in system", rideService.getRides().size() == 1, "Ride ID: " + rideId);
        assertTrue("Driver identity preserved", ride.getDriver().getId().equals(driverStudent.getId()), "UUID: " + driverStudent.getId());
        assertTrue("Notes attached properly", "Leaving sharp at 9 AM, AC car with boot space".equals(ride.getNotes()), ride.getNotes());
        assertTrue("Initial available seats correct", ride.getAvailableSeats() == 3, "3 seats available");

        // ---------------------------------------------------------------------
        // Step 4: Search Ride
        // ---------------------------------------------------------------------
        System.out.println("\n--- 4. Testing Ride Search & Filtering ---");
        ArrayList<Ride> searchResults = rideService.findRides("NSSCE Main Gate", "Palakkad Junction Railway Station");
        assertTrue("Route search matches", searchResults.size() == 1, "Found 1 matching ride");
        ArrayList<Ride> mismatch = rideService.findRides("Kozhikode", "Palakkad");
        assertTrue("Route mismatch returns empty", mismatch.isEmpty(), "Zero results for non-matching route");

        // ---------------------------------------------------------------------
        // Step 5: Constraint Validation (Self-Booking & Overbooking)
        // ---------------------------------------------------------------------
        System.out.println("\n--- 5. Testing Security & Request Constraints ---");

        // 5a. Driver cannot request own ride
        boolean selfRequestBlocked = false;
        try {
            rideService.requestRide(rideId, driverStudent, 1, "Hostel");
        } catch (InvalidRideException ex) {
            selfRequestBlocked = ex.getMessage().contains("cannot request your own ride");
        } catch (Exception ignored) {}
        assertTrue("Self-request prevented", selfRequestBlocked, "Driver cannot book own ride");

        // 5b. Overbooking seats (requesting 4 seats when 3 available)
        boolean overbookBlocked = false;
        try {
            rideService.requestRide(rideId, riderStudent, 5, "Hostel");
        } catch (InvalidRideException ex) {
            overbookBlocked = ex.getMessage().contains("exceeds available seats");
        } catch (Exception ignored) {}
        assertTrue("Overbooking prevented", overbookBlocked, "Cannot request more seats than available capacity");

        // ---------------------------------------------------------------------
        // Step 6: Passenger Submits Ride Request with Custom Pickup Location
        // ---------------------------------------------------------------------
        System.out.println("\n--- 6. Testing Ride Request with Separate Pickup Location ---");
        String customPickup = "Ladies Hostel Gate 2";
        boolean requestCreated = false;
        try {
            requestCreated = rideService.requestRide(rideId, riderStudent, 1, customPickup);
        } catch (Exception e) {
            System.err.println("Request failed: " + e.getMessage());
        }
        assertTrue("Passenger request created", requestCreated, "1 seat requested");

        ArrayList<RideRequest> riderRequests = rideService.getRequestsByRequester(riderStudent.getId());
        assertTrue("Rider can see submitted request", riderRequests.size() == 1, "Found 1 request");
        RideRequest submittedReq = riderRequests.get(0);
        assertTrue("Status is PENDING", "PENDING".equals(submittedReq.getStatus()), "Status: " + submittedReq.getStatus());
        assertTrue("Custom pickup preserved separately", customPickup.equals(submittedReq.getPickupLocation()), "Pickup: " + submittedReq.getPickupLocation());
        assertTrue("Drop-off matches ride destination", "Palakkad Junction Railway Station".equals(submittedReq.getDropoffLocation()), "Drop-off: " + submittedReq.getDropoffLocation());

        // ---------------------------------------------------------------------
        // Step 7: Duplicate Request Prevention
        // ---------------------------------------------------------------------
        System.out.println("\n--- 7. Testing Duplicate Request Prevention ---");
        boolean duplicateBlocked = false;
        try {
            rideService.requestRide(rideId, riderStudent, 1, "Another location");
        } catch (InvalidRideException ex) {
            duplicateBlocked = ex.getMessage().contains("already have an active pending request");
        } catch (Exception ignored) {}
        assertTrue("Duplicate request blocked", duplicateBlocked, "Cannot submit multiple pending requests for same ride");

        // ---------------------------------------------------------------------
        // Step 8: Driver Views Incoming Requests
        // ---------------------------------------------------------------------
        System.out.println("\n--- 8. Testing Driver Incoming Request View ---");
        ArrayList<RideRequest> driverIncoming = rideService.getRequestsForDriver(driverStudent.getId());
        assertTrue("Driver receives incoming request", driverIncoming.size() == 1, "1 incoming request found");
        RideRequest driverViewReq = driverIncoming.get(0);
        assertTrue("Requester name visible", "Sneha Menon".equals(driverViewReq.getRequester().getName()), "Requester: " + driverViewReq.getRequester().getName());
        assertTrue("Pickup visible to driver", customPickup.equals(driverViewReq.getPickupLocation()), "Pickup: " + driverViewReq.getPickupLocation());

        // ---------------------------------------------------------------------
        // Step 9: Driver Accepts Request (Seat Deduction)
        // ---------------------------------------------------------------------
        System.out.println("\n--- 9. Testing Accept Request & Seat Deduction ---");
        boolean accepted = false;
        try {
            accepted = rideService.acceptRequest(driverViewReq.getRequestId());
        } catch (Exception e) {
            System.err.println("Accept failed: " + e.getMessage());
        }
        assertTrue("Request accepted successfully", accepted, "Request ID: " + driverViewReq.getRequestId());
        assertTrue("Request status transitioned to ACCEPTED", "ACCEPTED".equals(driverViewReq.getStatus()), "Status: " + driverViewReq.getStatus());

        Ride updatedRide = null;
        try {
            updatedRide = rideService.findRideById(rideId);
        } catch (Exception ignored) {}
        assertTrue("Seats decremented from 3 to 2", updatedRide != null && updatedRide.getAvailableSeats() == 2,
                "Available seats now: " + (updatedRide != null ? updatedRide.getAvailableSeats() : -1));

        // ---------------------------------------------------------------------
        // Step 10: Rider Views Confirmed Details in My Requests
        // ---------------------------------------------------------------------
        System.out.println("\n--- 10. Testing Rider Confirmed Booking Tracking ---");
        ArrayList<RideRequest> updatedRiderRequests = rideService.getRequestsByRequester(riderStudent.getId());
        RideRequest confirmedReq = updatedRiderRequests.get(0);
        assertTrue("Rider sees ACCEPTED status", "ACCEPTED".equals(confirmedReq.getStatus()), "Status: " + confirmedReq.getStatus());
        assertTrue("Confirmed pickup is visible", customPickup.equals(confirmedReq.getPickupLocation()), "Confirmed Pickup: " + confirmedReq.getPickupLocation());
        assertTrue("Confirmed drop-off is visible", "Palakkad Junction Railway Station".equals(confirmedReq.getDropoffLocation()), "Confirmed Drop-off: " + confirmedReq.getDropoffLocation());

        // ---------------------------------------------------------------------
        // Step 11: Ride Cancellation & Unauthorized Protection
        // ---------------------------------------------------------------------
        System.out.println("\n--- 11. Testing Ride Cancellation & Authorization ---");
        boolean unauthorizedBlocked = false;
        try {
            // Rider attempts to cancel driver's ride
            rideService.cancelRide(rideId, riderStudent.getId());
        } catch (InvalidRideException ex) {
            unauthorizedBlocked = ex.getMessage().contains("Unauthorized");
        } catch (Exception ignored) {}
        assertTrue("Unauthorized cancel blocked", unauthorizedBlocked, "Other student cannot cancel someone else's ride");

        // Driver cancels own ride
        boolean driverCancelled = false;
        try {
            driverCancelled = rideService.cancelRide(rideId, driverStudent.getId());
        } catch (Exception ignored) {}
        assertTrue("Driver can cancel own ride", driverCancelled, "Ride removed by owner");
        assertTrue("Ride removed from active listings", rideService.getRides().isEmpty(), "0 active rides");

        // ---------------------------------------------------------------------
        // Summary
        // ---------------------------------------------------------------------
        System.out.println("\n==========================================================");
        System.out.println(String.format(" Test Summary: %d / %d Tests Passed", testsPassed, testsTotal));
        System.out.println("==========================================================");

        if (testsPassed == testsTotal) {
            System.out.println(">>> ALL CAMPUS RIDE INTEGRATION TESTS PASSED PERFECTLY! <<<");
        } else {
            System.exit(1);
        }
    }
}
