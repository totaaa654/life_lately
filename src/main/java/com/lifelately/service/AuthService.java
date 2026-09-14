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
        return createAccount(username, displayName, password, confirmation);
    }

    public User createAccount(String username, String displayName, String password, String confirmation) {
        String cleanUsername = validateUsername(username);
        String cleanDisplayName = validateDisplayName(displayName);
        validatePassword(password, confirmation);
        if (userDAO.findByUsername(cleanUsername).isPresent()) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        User user = userDAO.insert(cleanUsername, cleanDisplayName, hash(password, salt),
                Base64.getEncoder().encodeToString(salt));
        userDAO.claimUnownedEntries(user.id());
        currentUser = user;
        userDAO.rememberUser(user.id());
        return user;
    }

    public User updateDisplayName(String displayName) {
        User user = requireCurrentUser();
        currentUser = userDAO.updateDisplayName(user.id(), validateDisplayName(displayName));
        return currentUser;
    }

    public void changePassword(String currentPassword, String newPassword, String confirmation) {
        User user = requireCurrentUser();
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password is required.");
        }
        if (!passwordMatches(user, currentPassword)) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        validatePassword(newPassword, confirmation);
        if (passwordMatches(user, newPassword)) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        currentUser = userDAO.updatePassword(user.id(), hash(newPassword, salt),
                Base64.getEncoder().encodeToString(salt));
    }

    public User login(String username, String password) {
        String cleanUsername = ValidationUtils.requireText(username, "Username");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required.");
        User user = userDAO.findByUsername(cleanUsername)
                .orElseThrow(() -> new IllegalArgumentException("Username or password is incorrect."));
        if (!passwordMatches(user, password)) {
            throw new IllegalArgumentException("Username or password is incorrect.");
        }
        userDAO.recordLogin(user.id());
        currentUser = user;
        userDAO.rememberUser(user.id());
        return user;
    }

    public boolean restoreSession() {
        if (currentUser != null) return true;
        currentUser = userDAO.findRememberedUser().orElse(null);
        return currentUser != null;
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public void logout() {
        userDAO.clearRememberedUser();
        currentUser = null;
    }

    private String validateUsername(String username) {
        String clean = ValidationUtils.requireText(username, "Username");
        int length = clean.codePointCount(0, clean.length());
        if (length < 2 || length > 40) {
            throw new IllegalArgumentException("Username must be 2-40 characters.");
        }
        if (clean.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Username cannot contain line breaks or control characters.");
        }
        return clean;
    }

    private String validateDisplayName(String displayName) {
        String clean = ValidationUtils.requireText(displayName, "Display name");
        if (clean.codePointCount(0, clean.length()) > 80) {
            throw new IllegalArgumentException("Display name must be 80 characters or fewer.");
        }
        return clean;
    }

    private void validatePassword(String password, String confirmation) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (password.length() < 8 || password.length() > 128) {
            throw new IllegalArgumentException("Password must be 8-128 characters.");
        }
        if (password.codePoints().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Password cannot contain spaces.");
        }
        if (password.codePoints().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("Password needs at least one uppercase letter.");
        }
        if (password.codePoints().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("Password needs at least one lowercase letter.");
        }
        if (password.codePoints().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Password needs at least one number.");
        }
        if (password.codePoints().noneMatch(character -> !Character.isLetterOrDigit(character))) {
            throw new IllegalArgumentException("Password needs at least one symbol.");
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
    }

    private User requireCurrentUser() {
        return currentUser().orElseThrow(() -> new IllegalStateException("Sign in to manage your account."));
    }

    private boolean passwordMatches(User user, String password) {
        byte[] salt = Base64.getDecoder().decode(user.passwordSalt());
        byte[] expected = Base64.getDecoder().decode(user.passwordHash());
        byte[] actual = Base64.getDecoder().decode(hash(password, salt));
        return MessageDigest.isEqual(expected, actual);
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
