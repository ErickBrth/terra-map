package com.terramap.domain.model;

import java.util.Objects;

/**
 * Immutable value object holding advertiser contact details.
 */
public final class ContactInfo {

    private final String name;
    private final String email;
    private final String phone;

    public ContactInfo(String name, String email, String phone) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(email, "email must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("contact name must not be blank");
        }
        if (email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("contact email is invalid: " + email);
        }
        this.name = name.strip();
        this.email = email.strip().toLowerCase();
        this.phone = phone != null ? phone.strip() : null;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    /** Returns the email with everything before the first dot after @ masked. */
    public String getMaskedEmail() {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactInfo other)) return false;
        return name.equals(other.name) && email.equals(other.email)
                && Objects.equals(phone, other.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, phone);
    }

    @Override
    public String toString() {
        return "ContactInfo{name='" + name + "', email='" + getMaskedEmail() + "'}";
    }
}
