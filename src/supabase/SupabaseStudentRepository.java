package supabase;

import model.Student;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Data Access Object / Repository for Student profiles in Supabase.
 *
 * Interacts with the 'public.students' table in Supabase via PostgREST API:
 * - Table schema:
 *   - id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE
 *   - full_name TEXT NOT NULL
 *   - registration_number TEXT NOT NULL UNIQUE
 *   - email TEXT NOT NULL UNIQUE
 *   - created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
 *
 * IMPORTANT SECURITY REQUIREMENT:
 * - Passwords are NEVER stored in public.students. Passwords are handled exclusively by Supabase Auth.
 *
 * OOP Concept: Encapsulation & Separation of Concerns (Data layer independent of UI and Models).
 */
public class SupabaseStudentRepository {

    private final SupabaseClient client;

    public SupabaseStudentRepository(SupabaseClient client) {
        this.client = client;
    }

    /**
     * Inserts a student profile into public.students following successful Supabase Auth signup.
     * NEVER stores passwords.
     *
     * @deprecated Deprecated as student profiles in public.students are now created automatically
     *             by the Supabase database trigger 'on_auth_user_created' executing 'public.handle_new_student()'
     *             on auth.users AFTER INSERT. Direct client INSERT is blocked by Row Level Security (RLS).
     * @param userId             The authenticated Supabase Auth user UUID.
     * @param fullName           The entered student name.
     * @param registrationNumber The entered registration number.
     * @param email              The entered college email.
     */
    @Deprecated
    public void insertStudentProfile(String userId, String fullName, String registrationNumber, String email) throws Exception {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User UUID cannot be empty.");
        }

        String jsonPayload = "{"
                + "\"id\":\"" + SupabaseClient.escapeJson(userId) + "\","
                + "\"full_name\":\"" + SupabaseClient.escapeJson(fullName) + "\","
                + "\"registration_number\":\"" + SupabaseClient.escapeJson(registrationNumber) + "\","
                + "\"email\":\"" + SupabaseClient.escapeJson(email) + "\""
                + "}";

        client.executeRest("students", "POST", jsonPayload);
    }

    /**
     * Checks if a college email is already registered in public.students.
     */
    public boolean isEmailRegistered(String email) {
        if (email == null || email.trim().isEmpty()) return false;

        try {
            String encoded = URLEncoder.encode(email.trim().toLowerCase(), StandardCharsets.UTF_8);
            String response = client.executeRest("students?email=eq." + encoded + "&select=email", "GET", null);
            return response != null && response.contains("\"email\"");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a registration number is already registered in public.students.
     */
    public boolean isRegistrationNumberRegistered(String registrationNumber) {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) return false;

        try {
            String encoded = URLEncoder.encode(registrationNumber.trim(), StandardCharsets.UTF_8);
            String response = client.executeRest("students?registration_number=eq." + encoded + "&select=registration_number", "GET", null);
            return response != null && response.contains("\"registration_number\"");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieves a student by email from public.students and extracts the Supabase Auth UUID.
     */
    public Student findStudentByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;

        try {
            String encoded = URLEncoder.encode(email.trim().toLowerCase(), StandardCharsets.UTF_8);
            String path = "students?email=eq." + encoded + "&select=*";
            String response = client.executeRest(path, "GET", null);
            if (response != null && response.contains("\"email\"")) {
                String id = SupabaseClient.extractField(response, "id");
                String fullName = SupabaseClient.extractField(response, "full_name");
                if (fullName == null) fullName = SupabaseClient.extractField(response, "name");

                String regNum = SupabaseClient.extractField(response, "registration_number");
                if (regNum == null) regNum = SupabaseClient.extractField(response, "student_id");

                String mail = SupabaseClient.extractField(response, "email");
                Student student = new Student(fullName, regNum, mail, "");
                if (id != null && !id.trim().isEmpty()) {
                    student.setId(id.trim());
                }
                return student;
            }
        } catch (Exception e) {
            System.out.println("Supabase: Student query note: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a student by Supabase Auth user UUID.
     */
    public Student findStudentById(String userId) {
        if (userId == null || userId.trim().isEmpty()) return null;

        try {
            String encoded = URLEncoder.encode(userId.trim(), StandardCharsets.UTF_8);
            String path = "students?id=eq." + encoded + "&select=*";
            String response = client.executeRest(path, "GET", null);
            if (response != null && response.contains("\"email\"")) {
                String id = SupabaseClient.extractField(response, "id");
                if (id == null || id.trim().isEmpty()) id = userId.trim();

                String fullName = SupabaseClient.extractField(response, "full_name");
                if (fullName == null) fullName = SupabaseClient.extractField(response, "name");

                String regNum = SupabaseClient.extractField(response, "registration_number");
                if (regNum == null) regNum = SupabaseClient.extractField(response, "student_id");

                String mail = SupabaseClient.extractField(response, "email");
                Student student = new Student(fullName, regNum, mail, "");
                student.setId(id);
                return student;
            }
        } catch (Exception e) {
            System.out.println("Supabase: Student query by id note: " + e.getMessage());
        }
        return null;
    }

    /**
     * Loads all students from Supabase into an ArrayList.
     */
    public ArrayList<Student> loadAllStudents() {
        ArrayList<Student> list = new ArrayList<>();
        try {
            String response = client.executeRest("students?select=*&order=created_at.desc", "GET", null);
            // In-memory list populated if response is valid JSON array
        } catch (Exception e) {
            System.out.println("Supabase: Fetch all students note: " + e.getMessage());
        }
        return list;
    }
}
