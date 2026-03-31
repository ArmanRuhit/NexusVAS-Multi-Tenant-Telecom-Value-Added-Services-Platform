package dev.armanruhit.nexusvas.common_lib.dto;


public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorResponse error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(true, data, null);
    }


    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<T>(false, null, error);
    }
}
