package com.lifelately.model;

public record Tag(long id, String name, String colorHex) {
    @Override
    public String toString() {
        return name;
    }
}
