package com.lifelately.service;

import com.lifelately.dao.UserDAO;
import com.lifelately.model.User;
import com.lifelately.util.ValidationUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Optional;

public final class AuthService {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;

    private final UserDAO userDAO;
    private User currentUser;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public boolean requiresFirstAccount() {
        return !userDAO.hasUsers();
    }

    public User createFirstAccount(String username, String displayName, String password, String confirmation) {
        if (userDAO.hasUsers()) {
            throw new IllegalArgumentException("A local account already exists. Sign in instead.");
        }
        String cleanUsername = validateUsername(username);
        String cleanDisplayName = ValidationUtils.requireText(displayName, "Display name");
        validatePassword(password, confirmation);
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        User user = userDAO.insert(cleanUsername, cleanDisplayName, hash(password, salt),
                Base64.getEncoder().encodeToString(salt));
        currentUser = user;
        return user;
    }

    public User login(String username, String password) {
        String cleanUsername = ValidationUtils.requireText(username, "Username");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required.");
        User user = userDAO.findByUsername(cleanUsername)
                .orElseThrow(() -> new IllegalArgumentException("Username or password is incorrect."));
        byte[] salt = Base64.getDecoder().decode(user.passwordSalt());
        byte[] expected = Base64.getDecoder().decode(user.passwordHash());
        byte[] actual = Base64.getDecoder().decode(hash(password, salt));
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("Username or password is incorrect.");
        }
        userDAO.recordLogin(user.id());
        currentUser = user;
        return user;
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public void logout() {
        currentUser = null;
    }

    private String validateUsername(String username) {
        String clean = ValidationUtils.requireText(username, "Username");
        if (!clean.matches("[A-Za-z0-9._-]{3,40}")) {
            throw new IllegalArgumentException("Username must be 3-40 letters, numbers, dots, dashes, or underscores.");
        }
        return clean;
    }

    private void validatePassword(String password, String confirmation) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
    }

    private String hash(String password, byte[] salt) {
        PBEKeySpec specification = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification).getEncoded();
            return Base64.getEncoder().encodeToString(encoded);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Password protection is unavailable.", exception);
        } finally {
            specification.clearPassword();
        }
    }
}
