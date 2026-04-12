package com.example.bookexchange.common.storage;

public record ProcessedImage(
        byte[] bytes,
        String contentType,
        String extension
) {
}
