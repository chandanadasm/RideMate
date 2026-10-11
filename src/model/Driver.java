package model;

/**
 * OOP Concept: Multilevel Inheritance (Driver extends Student, which extends User)
 * OOP Concept: Aggregation / Composition (Driver has a Vehicle)
 * Represents a student who can offer rides as a driver.
 */
public class Driver extends Student {

    // Aggregation: Driver has-a Vehicle
    private Vehicle vehicle;

    // Default constructor
    public Driver() {
        super();
    }

    // Parameterized constructor using super() chaining
    public Driver(String name, String studentId, String email, String password, Vehicle vehicle) {
        super(name, studentId, email, password);
        this.vehicle = vehicle;
    }

    // Parameterized constructor including Supabase user UUID
    public Driver(String id, String name, String studentId, String email, String password, Vehicle vehicle) {
        super(id, name, studentId, email, password);
        this.vehicle = vehicle;
    }

    // Getter and Setter
    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    // OOP Concept: Method Overriding (overrides displayProfile from Student/User)
    @Override
    public void displayProfile() {
        super.displayProfile();
        System.out.println("Role: Driver");
        if (vehicle != null) {
            vehicle.displayVehicleDetails();
        }
    }
}
