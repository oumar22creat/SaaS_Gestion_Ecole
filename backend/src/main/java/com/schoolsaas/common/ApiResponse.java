package com.schoolsaas.common;

/** Enveloppe de réponse succès uniforme — voir docs/API_CONVENTIONS.md. */
public record ApiResponse<T>(T data, PageMeta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> of(T data, PageMeta meta) {
        return new ApiResponse<>(data, meta);
    }

    public record PageMeta(int page, int pageSize, long total) {
    }
}
