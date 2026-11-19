package ua.kpi.absolutesvinema.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * BCrypt with an additional application-wide secret ("pepper").
 * <p>
 * BCrypt still generates its own random salt for every password. The pepper (PASSWORD_SALT from .env)
 * is applied first as HMAC-SHA256(pepper, rawPassword); the Base64 result (44 chars, below BCrypt's
 * 72-byte limit) is what BCrypt hashes. A leaked database alone is therefore not enough to brute-force
 * passwords. Changing or losing the pepper makes every stored hash unverifiable.
 */
public class PepperedPasswordEncoder implements PasswordEncoder {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec pepperKey;
    private final PasswordEncoder bcrypt;

    public PepperedPasswordEncoder(String pepper) {
        this(pepper, new BCryptPasswordEncoder());
    }

    PepperedPasswordEncoder(String pepper, PasswordEncoder bcrypt) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException("PASSWORD_SALT must be configured with a non-empty value");
        }
        this.pepperKey = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.bcrypt = bcrypt;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(prehash(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null) {
            return false;
        }
        return bcrypt.matches(prehash(rawPassword), encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return bcrypt.upgradeEncoding(encodedPassword);
    }

    private String prehash(CharSequence rawPassword) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(pepperKey);
            byte[] digest = mac.doFinal(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
