package ua.kpi.absolutesvinema.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PepperedPasswordEncoderTest {

    private static final String PEPPER = "unit-test-pepper";

    private final PepperedPasswordEncoder encoder = new PepperedPasswordEncoder(PEPPER);

    @Test
    void encodedPasswordMatchesOnlyTheOriginalPassword() {
        String hash = encoder.encode("user123");

        assertThat(hash).startsWith("$2a$");
        assertThat(encoder.matches("user123", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
        assertThat(encoder.matches(null, hash)).isFalse();
    }

    @Test
    void bcryptStillUsesRandomSaltPerHash() {
        assertThat(encoder.encode("user123")).isNotEqualTo(encoder.encode("user123"));
    }

    @Test
    void hashDoesNotExposePepperOrPassword() {
        String hash = encoder.encode("user123");

        assertThat(hash).doesNotContain(PEPPER).doesNotContain("user123");
    }

    @Test
    void differentPepperCannotVerifyExistingHash() {
        String hash = encoder.encode("user123");

        assertThat(new PepperedPasswordEncoder("another-pepper").matches("user123", hash)).isFalse();
    }

    @Test
    void plainBcryptHashWithoutPepperIsRejected() {
        String plainHash = new BCryptPasswordEncoder().encode("user123");

        assertThat(encoder.matches("user123", plainHash)).isFalse();
    }

    @Test
    void blankPepperIsRejectedWithoutLeakingValue() {
        assertThatThrownBy(() -> new PepperedPasswordEncoder("  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PASSWORD_SALT must be configured with a non-empty value");
        assertThatThrownBy(() -> new PepperedPasswordEncoder(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
