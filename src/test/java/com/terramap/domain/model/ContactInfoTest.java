package com.terramap.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactInfoTest {

    @Test
    void createsValidContact() {
        ContactInfo contact = new ContactInfo("Jane Doe", "Jane@Example.com", " +55 11 90000-0000 ");

        assertThat(contact.getName()).isEqualTo("Jane Doe");
        assertThat(contact.getEmail()).isEqualTo("jane@example.com"); // lowercased
        assertThat(contact.getPhone()).isEqualTo("+55 11 90000-0000"); // trimmed
    }

    @Test
    void phoneIsOptional() {
        ContactInfo contact = new ContactInfo("Jane Doe", "jane@example.com", null);

        assertThat(contact.getPhone()).isNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new ContactInfo("   ", "jane@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsEmailWithoutAtSign() {
        assertThatThrownBy(() -> new ContactInfo("Jane Doe", "not-an-email", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> new ContactInfo("Jane Doe", "  ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new ContactInfo(null, "jane@example.com", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullEmail() {
        assertThatThrownBy(() -> new ContactInfo("Jane Doe", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void masksEmailKeepingFirstCharacterAndDomain() {
        ContactInfo contact = new ContactInfo("Jane Doe", "jane@example.com", null);

        assertThat(contact.getMaskedEmail()).isEqualTo("j***@example.com");
    }

    @Test
    void masksVeryShortLocalPartSafely() {
        // local part has length 1 -> "at index" <= 1, mask falls back to returning the email as-is
        ContactInfo contact = new ContactInfo("Jane Doe", "a@example.com", null);

        assertThat(contact.getMaskedEmail()).isEqualTo("a@example.com");
    }

    @Test
    void toStringNeverExposesRawEmail() {
        ContactInfo contact = new ContactInfo("Jane Doe", "jane@example.com", null);

        assertThat(contact.toString()).doesNotContain("jane@example.com");
        assertThat(contact.toString()).contains("j***@example.com");
    }

    @Test
    void equalsAndHashCodeConsiderAllFields() {
        ContactInfo a = new ContactInfo("Jane Doe", "jane@example.com", "123");
        ContactInfo b = new ContactInfo("Jane Doe", "jane@example.com", "123");
        ContactInfo c = new ContactInfo("Jane Doe", "jane@example.com", "456");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}
