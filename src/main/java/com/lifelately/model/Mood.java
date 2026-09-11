package com.lifelately.model;

public record Mood(long id, String name, String icon, String colorHex, int sortOrder) {
    @Override
    public String toString() {
        return name;
    }
}
