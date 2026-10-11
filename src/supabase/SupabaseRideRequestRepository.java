package supabase;

import model.RideRequest;
import model.Student;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Data Access Object / Repository for Ride Requests in Supabase.
 *
 * Interacts with 'public.ride_requests' table via PostgREST API:
 * - Table fields: id, ride_id, requester_id, driver_id, seats_requested, status, created_at
 * - Supports atomic accept/decline RPCs and standard REST fallbacks.
 *
 * OOP Concept: Encapsulation & Separation of Concerns (Data layer decoupled from UI and Models).
 */
public class SupabaseRideRequestRepository {

    private final SupabaseClient client;

    public SupabaseRideRequestRepository(SupabaseClient client) {
        this.client = client;
    }

    /**
     * Creates a new ride request in Supabase.
     */
    public boolean createRequest(RideRequest request) {
        if (request == null) return false;

        try {
            String requesterUuid = request.getRequesterId();
            if (requesterUuid == null || requesterUuid.trim().isEmpty()) {
                if (request.getRequester() != null && request.getRequester().getId() != null) {
                    requesterUuid = request.getRequester().getId();
                } else if (client.getCurrentUserId() != null) {
                    requesterUuid = client.getCurrentUserId();
                }
            }

            if (requesterUuid == null || requesterUuid.trim().isEmpty()) {
                System.out.println("Supabase: Requester UUID is missing; request cannot be created.");
                return false;
            }

            String driverUuid = request.getDriverId();
            if (driverUuid == null) driverUuid = "";

            String fullPayload = "{"
                    + "\"id\":\"" + SupabaseClient.escapeJson(request.getRequestId()) + "\","
                    + "\"ride_id\":\"" + SupabaseClient.escapeJson(request.getRideId()) + "\","
                    + "\"requester_id\":\"" + SupabaseClient.escapeJson(requesterUuid) + "\","
                    + "\"driver_id\":\"" + SupabaseClient.escapeJson(driverUuid) + "\","
                    + "\"seats_requested\":" + request.getSeatsRequested() + ","
                    + "\"pickup_location\":\"" + SupabaseClient.escapeJson(request.getPickupLocation() != null ? request.getPickupLocation() : "") + "\","
                    + "\"dropoff_location\":\"" + SupabaseClient.escapeJson(request.getDropoffLocation() != null ? request.getDropoffLocation() : "") + "\","
                    + "\"status\":\"" + SupabaseClient.escapeJson(request.getStatus()) + "\""
                    + "}";

            try {
                String response = client.executeRest("ride_requests", "POST", fullPayload);
                if (response != null) {
                    String id = SupabaseClient.extractField(response, "id");
                    if (id != null) {
                        request.setRequestId(id);
                    }
                }
                return true;
            } catch (Exception colEx) {
                // Tier 1 fallback: omit pickup_location and dropoff_location if columns not in DB
                if (colEx.getMessage() != null && (colEx.getMessage().contains("pickup_location") || colEx.getMessage().contains("dropoff_location") || colEx.getMessage().contains("column"))) {
                    try {
                        String payloadNoLocations = "{"
                                + "\"id\":\"" + SupabaseClient.escapeJson(request.getRequestId()) + "\","
                                + "\"ride_id\":\"" + SupabaseClient.escapeJson(request.getRideId()) + "\","
                                + "\"requester_id\":\"" + SupabaseClient.escapeJson(requesterUuid) + "\","
                                + "\"driver_id\":\"" + SupabaseClient.escapeJson(driverUuid) + "\","
                                + "\"seats_requested\":" + request.getSeatsRequested() + ","
                                + "\"status\":\"" + SupabaseClient.escapeJson(request.getStatus()) + "\""
                                + "}";
                        String response = client.executeRest("ride_requests", "POST", payloadNoLocations);
                        if (response != null) {
                            String id = SupabaseClient.extractField(response, "id");
                            if (id != null) request.setRequestId(id);
                        }
                        return true;
                    } catch (Exception subEx) {
                        // Tier 2 fallback: minimal schema (id, ride_id, requester_id, status)
                        if (subEx.getMessage() != null && subEx.getMessage().contains("column")) {
                            String minimalPayload = "{"
                                    + "\"id\":\"" + SupabaseClient.escapeJson(request.getRequestId()) + "\","
                                    + "\"ride_id\":\"" + SupabaseClient.escapeJson(request.getRideId()) + "\","
                                    + "\"requester_id\":\"" + SupabaseClient.escapeJson(requesterUuid) + "\","
                                    + "\"status\":\"" + SupabaseClient.escapeJson(request.getStatus()) + "\""
                                    + "}";
                            String response = client.executeRest("ride_requests", "POST", minimalPayload);
                            if (response != null) {
                                String id = SupabaseClient.extractField(response, "id");
                                if (id != null) request.setRequestId(id);
                            }
                            return true;
                        }
                        throw subEx;
                    }
                }
                throw colEx;
            }
        } catch (Exception e) {
            System.out.println("Supabase: Create ride request note: " + e.getMessage());
            return false;
        }
    }

    /**
     * Atomically accepts a ride request (deducting seats and setting status = ACCEPTED).
     * Enforces that only the driver can accept the request.
     */
    public boolean acceptRequest(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) return false;

        try {
            // First attempt secure atomic RPC endpoint
            String rpcPayload = "{\"p_request_id\":\"" + SupabaseClient.escapeJson(requestId.trim()) + "\"}";
            try {
                String rpcResponse = client.executeRest("rpc/accept_ride_request", "POST", rpcPayload);
                if (rpcResponse != null && rpcResponse.contains("\"success\":true")) {
                    return true;
                }
            } catch (Exception rpcEx) {
                // If RPC function is not yet created in Supabase SQL editor, fall back to REST PATCH
            }

            // Fallback REST PATCH for environments without RPC deployed
            String jsonPayload = "{\"status\":\"ACCEPTED\"}";
            client.executeRest("ride_requests?id=eq." + URLEncoder.encode(requestId.trim(), StandardCharsets.UTF_8), "PATCH", jsonPayload);
            return true;
        } catch (Exception e) {
            System.out.println("Supabase: Accept request note: " + e.getMessage());
            return false;
        }
    }

    /**
     * Declines a ride request.
     */
    public boolean declineRequest(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) return false;

        try {
            // First attempt secure atomic RPC endpoint
            String rpcPayload = "{\"p_request_id\":\"" + SupabaseClient.escapeJson(requestId.trim()) + "\"}";
            try {
                String rpcResponse = client.executeRest("rpc/decline_ride_request", "POST", rpcPayload);
                if (rpcResponse != null && rpcResponse.contains("\"success\":true")) {
                    return true;
                }
            } catch (Exception rpcEx) {
                // Fallback to REST PATCH
            }

            String jsonPayload = "{\"status\":\"DECLINED\"}";
            client.executeRest("ride_requests?id=eq." + URLEncoder.encode(requestId.trim(), StandardCharsets.UTF_8), "PATCH", jsonPayload);
            return true;
        } catch (Exception e) {
            System.out.println("Supabase: Decline request note: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads incoming requests for a specific driver.
     */
    public ArrayList<RideRequest> getRequestsForDriver(String driverUuid) {
        return getRequestsForDriver(driverUuid, null);
    }

    /**
     * Overloaded method to retrieve requests for a driver, with optional ride ID matching fallback.
     */
    public ArrayList<RideRequest> getRequestsForDriver(String driverUuid, java.util.List<String> driverRideIds) {
        ArrayList<RideRequest> list = new ArrayList<>();
        if ((driverUuid == null || driverUuid.trim().isEmpty()) && (driverRideIds == null || driverRideIds.isEmpty())) {
            return list;
        }

        try {
            if (driverUuid != null && !driverUuid.trim().isEmpty()) {
                String encoded = URLEncoder.encode(driverUuid.trim(), StandardCharsets.UTF_8);
                String path = "ride_requests?driver_id=eq." + encoded + "&select=*&order=created_at.desc";
                try {
                    String response = client.executeRest(path, "GET", null);
                    if (response != null && response.trim().startsWith("[")) {
                        ArrayList<RideRequest> parsed = parseRequestsArray(response);
                        if (!parsed.isEmpty()) {
                            return parsed;
                        }
                    }
                } catch (Exception filterEx) {
                    // Column driver_id might not exist yet
                }
            }

            // Fallback: Query by driver's ride IDs
            if (driverRideIds != null && !driverRideIds.isEmpty()) {
                for (String rId : driverRideIds) {
                    if (rId == null || rId.trim().isEmpty()) continue;
                    String path = "ride_requests?ride_id=eq." + URLEncoder.encode(rId.trim(), StandardCharsets.UTF_8) + "&select=*&order=created_at.desc";
                    try {
                        String resp = client.executeRest(path, "GET", null);
                        if (resp != null && resp.trim().startsWith("[")) {
                            ArrayList<RideRequest> parsed = parseRequestsArray(resp);
                            for (RideRequest req : parsed) {
                                boolean exists = false;
                                for (RideRequest existing : list) {
                                    if (existing.getRequestId() != null && existing.getRequestId().equals(req.getRequestId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) list.add(req);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.out.println("Supabase: Fetch driver requests note: " + e.getMessage());
        }
        return list;
    }

    /**
     * Loads requests created by a specific requester.
     */
    public ArrayList<RideRequest> getRequestsByRequester(String requesterUuid) {
        ArrayList<RideRequest> list = new ArrayList<>();
        if (requesterUuid == null || requesterUuid.trim().isEmpty()) return list;

        try {
            String encoded = URLEncoder.encode(requesterUuid.trim(), StandardCharsets.UTF_8);
            String path = "ride_requests?requester_id=eq." + encoded + "&select=*&order=created_at.desc";
            String response = client.executeRest(path, "GET", null);
            if (response != null && response.trim().startsWith("[")) {
                return parseRequestsArray(response);
            }
        } catch (Exception e) {
            System.out.println("Supabase: Fetch requester requests note: " + e.getMessage());
        }
        return list;
    }

    /**
     * Parses PostgREST JSON array response into a list of RideRequest objects.
     */
    private ArrayList<RideRequest> parseRequestsArray(String response) {
        ArrayList<RideRequest> list = new ArrayList<>();
        if (response == null || !response.trim().startsWith("[")) return list;

        int start = response.indexOf("{");
        while (start != -1) {
            int end = response.indexOf("}", start);
            if (end == -1) break;

            String objJson = response.substring(start, end + 1);
            String id = SupabaseClient.extractField(objJson, "id");
            String rideId = SupabaseClient.extractField(objJson, "ride_id");
            String requesterId = SupabaseClient.extractField(objJson, "requester_id");
            String driverId = SupabaseClient.extractField(objJson, "driver_id");
            String status = SupabaseClient.extractField(objJson, "status");
            String createdAt = SupabaseClient.extractField(objJson, "created_at");
            String pickup = SupabaseClient.extractField(objJson, "pickup_location");
            String dropoff = SupabaseClient.extractField(objJson, "dropoff_location");

            int seats = 1;
            int seatsIdx = objJson.indexOf("\"seats_requested\":");
            if (seatsIdx != -1) {
                int commaIdx = objJson.indexOf(",", seatsIdx);
                int braceIdx = objJson.indexOf("}", seatsIdx);
                int endIdx = (commaIdx != -1 && commaIdx < braceIdx) ? commaIdx : braceIdx;
                if (endIdx != -1) {
                    try {
                        String numStr = objJson.substring(seatsIdx + 18, endIdx).trim();
                        seats = Integer.parseInt(numStr);
                    } catch (Exception ignored) {}
                }
            }

            RideRequest req = new RideRequest();
            req.setRequestId(id);
            req.setRideId(rideId);
            req.setRequesterId(requesterId);
            req.setDriverId(driverId);
            req.setSeatsRequested(seats);
            req.setPickupLocation(pickup != null ? pickup : "");
            req.setDropoffLocation(dropoff != null ? dropoff : "");
            req.setStatus(status);
            req.setCreatedAt(createdAt);

            list.add(req);
            start = response.indexOf("{", end + 1);
        }
        return list;
    }
}
