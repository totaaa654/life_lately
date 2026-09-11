package com.lifelately.model;

import java.time.LocalDateTime;

public record User(long id, String username, String displayName, String passwordHash,
                   String passwordSalt, LocalDateTime createdAt, LocalDateTime lastLoginAt) {
}
