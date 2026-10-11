package supabase;

import model.Driver;
import model.Ride;
import model.Vehicle;
import java.util.ArrayList;

/**
 * Data Access Object / Repository for Rides in Supabase.
 *
 * Interacts with the 'rides' table in Supabase via REST API:
 * - Table fields:
 *   id, driver_id, starting_location, destination, date, departure_time,
 *   vehicle_type, vehicle_number, available_seats, cost_per_person, created_at
 *
 * OOP Concept: Encapsulation & Separation of Concerns (Data layer independent of UI and Models).
 */
public class SupabaseRideRepository {

    private final SupabaseClient client;

    public SupabaseRideRepository(SupabaseClient client) {
        this.client = client;
    }

    /**
     * Persists a newly offered ride to Supabase.
     * Populates 'driver_id' with the authenticated driver's Supabase Auth user UUID.
     */
    public boolean saveRide(Ride ride) {
        if (ride == null) return false;

        try {
            // Prioritize driver UUID (matching public.students.id) over registration number
            String driverId = "";
            if (ride.getDriver() != null) {
                if (ride.getDriver().getId() != null && !ride.getDriver().getId().trim().isEmpty()) {
                    driverId = ride.getDriver().getId().trim();
                } else if (client.getCurrentUserId() != null && !client.getCurrentUserId().trim().isEmpty()) {
                    driverId = client.getCurrentUserId().trim();
                } else {
                    driverId = ride.getDriver().getStudentId();
                }
            } else if (client.getCurrentUserId() != null) {
                driverId = client.getCurrentUserId().trim();
            }

            // Verify logged-in session authorization
            if (client.getCurrentUserId() != null && !client.getCurrentUserId().trim().isEmpty()) {
                if (!driverId.isEmpty() && !driverId.equalsIgnoreCase(client.getCurrentUserId().trim())) {
                    System.out.println("Supabase authorization alert: Ride driver_id (" + driverId
                            + ") does not match authenticated user (" + client.getCurrentUserId() + "). Overriding to authenticated user.");
                    driverId = client.getCurrentUserId().trim();
                }
            }

            String rideUuid = ride.getRideId();
            if (rideUuid == null || !rideUuid.matches("^[0-9a-fA-F-]{36}$")) {
                rideUuid = java.util.UUID.randomUUID().toString();
            }

            // Attempt primary schema matching remote Supabase database (source, ride_date, ride_time, price_per_seat, notes)
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"id\":\"").append(SupabaseClient.escapeJson(rideUuid)).append("\",");
            sb.append("\"driver_id\":\"").append(SupabaseClient.escapeJson(driverId)).append("\",");
            sb.append("\"source\":\"").append(SupabaseClient.escapeJson(ride.getStartingLocation())).append("\",");
            sb.append("\"destination\":\"").append(SupabaseClient.escapeJson(ride.getDestination())).append("\",");
            sb.append("\"ride_date\":\"").append(SupabaseClient.escapeJson(ride.getDate())).append("\",");
            sb.append("\"ride_time\":\"").append(SupabaseClient.escapeJson(ride.getDepartureTime())).append("\",");
            sb.append("\"available_seats\":").append(ride.getAvailableSeats()).append(",");
            sb.append("\"price_per_seat\":").append(ride.getCostPerPerson());
            if (ride.getNotes() != null && !ride.getNotes().trim().isEmpty()) {
                sb.append(",\"notes\":\"").append(SupabaseClient.escapeJson(ride.getNotes().trim())).append("\"");
            }
            sb.append("}");

            try {
                client.executeRest("rides", "POST", sb.toString());
                return true;
            } catch (Exception colEx) {
                if (colEx.getMessage() != null && colEx.getMessage().contains("column")) {
                    // Fallback schema for environments using starting_location, date, departure_time, cost_per_person
                    String vehicleType = (ride.getDriver() != null && ride.getDriver().getVehicle() != null)
                            ? ride.getDriver().getVehicle().getVehicleType() : "Car";
                    String vehicleNumber = (ride.getDriver() != null && ride.getDriver().getVehicle() != null)
                            ? ride.getDriver().getVehicle().getVehicleNumber() : "";

                    String fallbackPayload = "{"
                            + "\"id\":\"" + SupabaseClient.escapeJson(rideUuid) + "\","
                            + "\"driver_id\":\"" + SupabaseClient.escapeJson(driverId) + "\","
                            + "\"starting_location\":\"" + SupabaseClient.escapeJson(ride.getStartingLocation()) + "\","
                            + "\"destination\":\"" + SupabaseClient.escapeJson(ride.getDestination()) + "\","
                            + "\"date\":\"" + SupabaseClient.escapeJson(ride.getDate()) + "\","
                            + "\"departure_time\":\"" + SupabaseClient.escapeJson(ride.getDepartureTime()) + "\","
                            + "\"vehicle_type\":\"" + SupabaseClient.escapeJson(vehicleType) + "\","
                            + "\"vehicle_number\":\"" + SupabaseClient.escapeJson(vehicleNumber) + "\","
                            + "\"available_seats\":" + ride.getAvailableSeats() + ","
                            + "\"cost_per_person\":" + ride.getCostPerPerson()
                            + "}";
                    client.executeRest("rides", "POST", fallbackPayload);
                    return true;
                }
                throw colEx;
            }
        } catch (Exception e) {
            System.out.println("Supabase: Ride save note: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates seat availability on a ride in Supabase when booked.
     */
    public boolean updateAvailableSeats(String rideId, int newSeats) {
        if (rideId == null) return false;

        try {
            String jsonPayload = "{\"available_seats\":" + newSeats + "}";
            client.executeRest("rides?id=eq." + SupabaseClient.escapeJson(rideId), "PATCH", jsonPayload);
            return true;
        } catch (Exception e) {
            System.out.println("Supabase: Update seats note: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes / cancels an offered ride from Supabase.
     */
    public boolean cancelRide(String rideId, String driverId) {
        if (rideId == null || rideId.trim().isEmpty()) return false;

        try {
            String path = "rides?id=eq." + java.net.URLEncoder.encode(rideId.trim(), java.nio.charset.StandardCharsets.UTF_8);
            if (driverId != null && !driverId.trim().isEmpty()) {
                path += "&driver_id=eq." + java.net.URLEncoder.encode(driverId.trim(), java.nio.charset.StandardCharsets.UTF_8);
            }
            client.executeRest(path, "DELETE", null);
            return true;
        } catch (Exception e) {
            System.out.println("Supabase: Cancel ride note: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads all active rides from Supabase and parses them into Ride model objects.
     */
    public ArrayList<Ride> loadAllRides() {
        ArrayList<Ride> list = new ArrayList<>();
        try {
            String response = client.executeRest("rides?select=*&order=created_at.desc", "GET", null);
            if (response != null && response.trim().startsWith("[")) {
                return parseRidesArray(response);
            }
        } catch (Exception e) {
            System.out.println("Supabase: Load rides note: " + e.getMessage());
        }
        return list;
    }

    /**
     * Parses PostgREST JSON array response into a list of Ride objects.
     */
    public ArrayList<Ride> parseRidesArray(String json) {
        ArrayList<Ride> list = new ArrayList<>();
        if (json == null || !json.trim().startsWith("[")) return list;

        int start = json.indexOf("{");
        while (start != -1) {
            int end = json.indexOf("}", start);
            if (end == -1) break;

            String objJson = json.substring(start, end + 1);
            String id = SupabaseClient.extractField(objJson, "id");
            String driverId = SupabaseClient.extractField(objJson, "driver_id");

            String source = SupabaseClient.extractField(objJson, "source");
            if (source == null) source = SupabaseClient.extractField(objJson, "starting_location");

            String dest = SupabaseClient.extractField(objJson, "destination");

            String rideDate = SupabaseClient.extractField(objJson, "ride_date");
            if (rideDate == null) rideDate = SupabaseClient.extractField(objJson, "date");

            String rideTime = SupabaseClient.extractField(objJson, "ride_time");
            if (rideTime == null) rideTime = SupabaseClient.extractField(objJson, "departure_time");

            int seats = extractInt(objJson, "available_seats", 1);
            double price = extractDouble(objJson, "price_per_seat", -1);
            if (price < 0) {
                price = extractDouble(objJson, "cost_per_person", 0.0);
            }

            String notes = SupabaseClient.extractField(objJson, "notes");
            String vType = SupabaseClient.extractField(objJson, "vehicle_type");
            if (vType == null || vType.isEmpty()) vType = "Car";
            String vNum = SupabaseClient.extractField(objJson, "vehicle_number");
            if (vNum == null) vNum = "";

            Vehicle vehicle = new Vehicle(vType, vNum, 4);
            Driver driver = new Driver(driverId, "Driver", "", "", "", vehicle);

            Ride ride = new Ride(id, driver, source, dest, rideDate, rideTime, seats, price, notes);
            list.add(ride);

            start = json.indexOf("{", end + 1);
        }
        return list;
    }

    private static int extractInt(String json, String key, int defaultVal) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx != -1) {
            int s = idx + pattern.length();
            while (s < json.length() && Character.isWhitespace(json.charAt(s))) s++;
            int e = s;
            while (e < json.length() && (Character.isDigit(json.charAt(e)) || json.charAt(e) == '-')) e++;
            if (e > s) {
                try {
                    return Integer.parseInt(json.substring(s, e).trim());
                } catch (Exception ignored) {}
            }
        }
        return defaultVal;
    }

    private static double extractDouble(String json, String key, double defaultVal) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx != -1) {
            int s = idx + pattern.length();
            while (s < json.length() && Character.isWhitespace(json.charAt(s))) s++;
            int e = s;
            while (e < json.length() && (Character.isDigit(json.charAt(e)) || json.charAt(e) == '.' || json.charAt(e) == '-')) e++;
            if (e > s) {
                try {
                    return Double.parseDouble(json.substring(s, e).trim());
                } catch (Exception ignored) {}
            }
        }
        return defaultVal;
    }
}
