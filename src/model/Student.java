package model;

import interfaces.Authenticatable;

/**
 * OOP Concept: Single Inheritance (extends User) & Interface Implementation (implements Authenticatable)
 * Represents a student registered in the CampusRide system.
 */
public class Student extends User implements Authenticatable {

    // Encapsulation: Private credential field
    private String password;

    // Default constructor
    public Student() {
        super();
    }

    // Parameterized constructor invoking parent constructor via super()
    public Student(String name, String studentId, String email, String password) {
        super(name, studentId, email);
        this.password = password;
    }

    // Parameterized constructor including Supabase user UUID
    public Student(String id, String name, String studentId, String email, String password) {
        super(id, name, studentId, email);
        this.password = password;
    }

    // Getter and Setter
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // OOP Concept: Method Overriding (overrides abstract method in User)
    @Override
    public void displayProfile() {
        displayBasicInfo();
        System.out.println("Role: Registered Student");
    }

    // OOP Concept: Interface Method Implementation (from Authenticatable)
    @Override
    public boolean login(String email, String password) {
        return this.getEmail() != null && this.getEmail().equalsIgnoreCase(email)
                && this.password != null && this.password.equals(password);
    }

    // OOP Concept: Interface Method Implementation (from Authenticatable)
    @Override
    public void logout() {
        System.out.println("Student " + getName() + " logged out.");
    }
}
