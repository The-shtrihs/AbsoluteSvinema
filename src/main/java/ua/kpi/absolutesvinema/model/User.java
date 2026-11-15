package ua.kpi.absolutesvinema.model;

public record User(Long id, String email, String password, String fullName, String role) {
    public User {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("User ID must be positive or null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name must not be blank");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role must not be blank");
        }
    }
}
