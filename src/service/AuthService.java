package service;

import exception.InvalidLoginException;
import model.Student;
import supabase.SupabaseClient;
import supabase.SupabaseStudentRepository;
import java.util.ArrayList;

/**
 * Service managing student registration, authentication, and session state.
 *
 * OOP Concepts Demonstrated:
 * 1. Collections Framework: Uses ArrayList<Student> as an in-memory dynamic list to store student objects.
 * 2. Exception Handling: Uses custom InvalidLoginException to handle authentication errors gracefully.
 * 3. Encapsulation: Protects student list and session state with private fields and controlled methods.
 */
public class AuthService {

    // Collections Framework: ArrayList allows dynamic resizing to store registered students
    private ArrayList<Student> students;

    // Encapsulation: Stores the currently authenticated student session (null if logged out)
    private Student currentLoggedInStudent;

    // Supabase Integration & Email Verification
    private SupabaseClient supabaseClient;
    private SupabaseStudentRepository studentRepo;
    private java.util.Set<String> verifiedEmails;

    // Constructor: Initializes the in-memory ArrayList and sets initial session to null
    public AuthService() {
        this.students = new ArrayList<>();
        this.currentLoggedInStudent = null;
        this.supabaseClient = new SupabaseClient();
        this.studentRepo = new SupabaseStudentRepository(supabaseClient);
        this.verifiedEmails = new java.util.HashSet<>();
    }

    /**
     * Registers a new student into the system and persists profile to Supabase.
     *
     * Flow:
     * 1. Validate all fields and verify @nssce.ac.in domain (case-insensitive).
     * 2. Ensure registration number and email are not duplicated.
     * 3. Call Supabase Auth signup with email, password, full_name, and registration_number metadata.
     * 4. Let the Supabase database trigger (on_auth_user_created) create public.students automatically.
     * 5. Never store passwords in public.students.
     *
     * @param student The Student object to register.
     * @return true if registration succeeded; throws InvalidLoginException on failure.
     */
    public boolean registerStudent(Student student) throws InvalidLoginException {
        // 1. Validate input object and fields
        if (student == null) {
            throw new InvalidLoginException("Registration failed: Invalid student information provided.");
        }
        if (student.getName() == null || student.getName().trim().isEmpty()) {
            throw new InvalidLoginException("Full Name cannot be empty.");
        }
        if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
            throw new InvalidLoginException("Registration Number cannot be empty.");
        }
        if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
            throw new InvalidLoginException("College email cannot be empty.");
        }
        if (student.getPassword() == null || student.getPassword().trim().isEmpty()) {
            throw new InvalidLoginException("Password cannot be empty.");
        }
        if (student.getPassword().length() < 6) {
            throw new InvalidLoginException("Password must be at least 6 characters long.");
        }

        String email = student.getEmail().trim().toLowerCase();
        String name = student.getName().trim();
        String regNum = student.getStudentId().trim();

        // 2. College Domain Validation: Only @nssce.ac.in addresses allowed (case-insensitive)
        if (!email.endsWith("@nssce.ac.in")) {
            throw new InvalidLoginException("Only @nssce.ac.in college email addresses are allowed.");
        }

        // 3. Local Duplicate Checks (in-memory collection cache)
        if (findStudentByEmail(email) != null) {
            throw new InvalidLoginException("A student with email '" + email + "' is already registered.");
        }
        for (Student s : students) {
            if (s.getStudentId() != null && s.getStudentId().equalsIgnoreCase(regNum)) {
                throw new InvalidLoginException("Registration number '" + regNum + "' is already registered.");
            }
        }

        // 4. Verify Supabase configuration is set
        if (!supabase.SupabaseConfig.isConfigured()) {
            throw new InvalidLoginException("Supabase is not configured yet.\nPlease configure your SUPABASE_URL and SUPABASE_ANON_KEY / publishable key in supabase.properties or environment variables.");
        }

        // 5. Check Supabase database for duplicates before signup
        if (studentRepo.isEmailRegistered(email)) {
            throw new InvalidLoginException("A student with email '" + email + "' is already registered in Supabase.");
        }
        if (studentRepo.isRegistrationNumberRegistered(regNum)) {
            throw new InvalidLoginException("Registration number '" + regNum + "' is already registered in Supabase.");
        }

        // 6. Supabase Auth Signup (delegates public.students creation to the database trigger)
        // The on_auth_user_created AFTER INSERT trigger executes public.handle_new_student(),
        // which automatically inserts the profile into public.students using auth.users.id,
        // raw_user_meta_data.full_name, raw_user_meta_data.registration_number, and auth.users.email.
        try {
            supabaseClient.signUp(email, student.getPassword(), name, regNum);
        } catch (Exception e) {
            throw new InvalidLoginException("Supabase Auth registration failed: " + e.getMessage());
        }

        String userUuid = supabaseClient.getCurrentUserId();
        if (userUuid == null || userUuid.trim().isEmpty()) {
            throw new InvalidLoginException("Registration failed: Did not receive user UUID from Supabase Auth.");
        }

        // Set the confirmed Supabase Auth UUID on the registered student model
        student.setId(userUuid.trim());

        // 7. Collections Framework: Add to in-memory ArrayList cache without plaintext password
        Student inMemoryStudent = new Student(name, regNum, email, "");
        inMemoryStudent.setId(userUuid.trim());
        students.add(inMemoryStudent);

        System.out.println("Registration successful! Supabase Auth user created (" + userUuid
                + ") and public.students profile automatically generated by on_auth_user_created trigger.");
        return true;
    }

    /**
     * Authenticates a student using Supabase Auth.
     * Does NOT authenticate by comparing passwords against public.students or local files.
     *
     * @param email    The student's college email.
     * @param password The student's password.
     * @return The authenticated Student object.
     * @throws InvalidLoginException if credentials, verification, or network fail.
     */
    public Student login(String email, String password) throws InvalidLoginException {
        // 1. Validate that fields are not empty
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new InvalidLoginException("Email and password cannot be empty.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 2. College Domain Validation: Only @nssce.ac.in addresses allowed
        if (!normalizedEmail.endsWith("@nssce.ac.in")) {
            throw new InvalidLoginException("Only @nssce.ac.in college email addresses are allowed.");
        }

        // 3. Verify Supabase configuration
        if (!supabase.SupabaseConfig.isConfigured()) {
            throw new InvalidLoginException("Supabase is not configured yet.\nPlease configure your SUPABASE_URL and SUPABASE_ANON_KEY in supabase.properties or environment variables.");
        }

        // 4. Authenticate strictly via Supabase Auth (NEVER by comparing passwords against public.students)
        try {
            supabaseClient.signInWithPassword(normalizedEmail, password);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("email not confirmed")) {
                throw new InvalidLoginException("Please verify your college email before logging in. A verification link was sent to " + normalizedEmail);
            } else if (msg != null && msg.toLowerCase().contains("invalid login credentials")) {
                throw new InvalidLoginException("Invalid login credentials. Please check your email and password.");
            } else {
                throw new InvalidLoginException("Login failed: " + (msg != null ? msg : "Authentication error"));
            }
        }

        // 5. Verification Check: User must confirm email before being allowed into dashboard
        if (!supabaseClient.isEmailConfirmed() && !isEmailVerified(normalizedEmail)) {
            throw new InvalidLoginException("Please verify your college email before logging in. A verification link was sent to " + normalizedEmail);
        }

        // 6. Fetch student profile from public.students table
        Student student = studentRepo.findStudentByEmail(normalizedEmail);
        if (student == null && supabaseClient.getCurrentUserId() != null) {
            student = studentRepo.findStudentById(supabaseClient.getCurrentUserId());
        }

        // If not found in table, check in-memory list or construct from Supabase user email
        if (student == null) {
            student = findStudentByEmail(normalizedEmail);
        }
        if (student == null) {
            student = new Student("NSSCE Student", "NSSCE", normalizedEmail, "");
        }

        // Safely map UUID from Supabase Auth session if not already populated
        if (student.getId() == null || student.getId().trim().isEmpty()) {
            String authUuid = supabaseClient.getCurrentUserId();
            if (authUuid != null && !authUuid.trim().isEmpty()) {
                student.setId(authUuid.trim());
            }
        }

        // 7. Update in-memory session and cache
        this.currentLoggedInStudent = student;
        if (findStudentByEmail(normalizedEmail) == null) {
            students.add(student);
        }
        markEmailAsVerified(normalizedEmail);

        System.out.println("Login successful via Supabase Auth! Welcome back, " + student.getName() + ".");
        return student;
    }

    /**
     * Logs out the currently authenticated student by clearing the session reference.
     */
    public void logout() {
        if (currentLoggedInStudent != null) {
            System.out.println("Goodbye, " + currentLoggedInStudent.getName() + ". You have been logged out successfully.");
            this.currentLoggedInStudent = null;
        } else {
            System.out.println("No student is currently logged in.");
        }
    }

    /**
     * Searches for a student by their email in the ArrayList.
     *
     * Collections Usage: Iterates over the ArrayList using an enhanced for-loop (for-each).
     *
     * @param email The email address to look for.
     * @return Matching Student object if found, or null if not found.
     */
    public Student findStudentByEmail(String email) {
        if (email == null) {
            return null;
        }

        // Enhanced for-loop traversing the ArrayList of students
        for (Student student : students) {
            // Case-insensitive comparison for email addresses
            if (student.getEmail() != null && student.getEmail().equalsIgnoreCase(email.trim())) {
                return student; // Student found in collection
            }
        }

        return null; // Student not found
    }

    // Checks if any student is currently logged in
    public boolean isLoggedIn() {
        return this.currentLoggedInStudent != null;
    }

    // Getters and Setters
    public ArrayList<Student> getStudents() {
        return students;
    }

    public void setStudents(ArrayList<Student> students) {
        this.students = students;
    }

    public Student getCurrentLoggedInStudent() {
        return currentLoggedInStudent;
    }

    public void setCurrentLoggedInStudent(Student currentLoggedInStudent) {
        this.currentLoggedInStudent = currentLoggedInStudent;
    }

    // Email verification state checking
    public boolean isEmailVerified(String email) {
        if (email == null) return false;
        String normalized = email.trim().toLowerCase();
        if (verifiedEmails.contains(normalized)) {
            return true;
        }
        if (supabase.SupabaseConfig.isConfigured() && supabaseClient.isEmailConfirmed()) {
            verifiedEmails.add(normalized);
            return true;
        }
        return false;
    }

    public void markEmailAsVerified(String email) {
        if (email != null) {
            verifiedEmails.add(email.trim().toLowerCase());
        }
    }

    public SupabaseClient getSupabaseClient() {
        return supabaseClient;
    }

    public SupabaseStudentRepository getStudentRepository() {
        return studentRepo;
    }
}
