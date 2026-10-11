package model;

/**
 * OOP Concept: Abstraction & Encapsulation
 * Abstract base class representing a general user in the CampusRide system.
 */
public abstract class User {

    // Encapsulation: Private member variables
    private String id;        // Supabase Auth user UUID (corresponds to public.students.id)
    private String name;
    private String studentId; // College registration number / roll number
    private String email;

    // Default constructor
    public User() {
    }

    // Parameterized constructor (backward compatible)
    public User(String name, String studentId, String email) {
        this.name = name;
        this.studentId = studentId;
        this.email = email;
    }

    // Parameterized constructor including Supabase user UUID
    public User(String id, String name, String studentId, String email) {
        this.id = id;
        this.name = name;
        this.studentId = studentId;
        this.email = email;
    }

    // OOP Concept: Abstraction - abstract method to be implemented by subclasses
    public abstract void displayProfile();

    // Concrete method shared by all subclasses
    public void displayBasicInfo() {
        System.out.println("Name: " + name + " | Student ID: " + studentId + " | Email: " + email
                + (id != null && !id.isEmpty() ? " | UUID: " + id : ""));
    }

    // Getters and Setters (Encapsulation)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Convenience alias for UUID matching database terminology
    public String getUuid() {
        return id;
    }

    public void setUuid(String uuid) {
        this.id = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Encapsulation: Database attribute aliases
    public String getFullName() {
        return name;
    }

    public void setFullName(String fullName) {
        this.name = fullName;
    }

    public String getRegistrationNumber() {
        return studentId;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.studentId = registrationNumber;
    }
}
