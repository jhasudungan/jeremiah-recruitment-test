package com.kopnus.jeremiah_recruitment_test.dto;

public record LoginResponse(String accessToken,
                            String tokenType,
                            long expiresIn   // seconds, OAuth2 token response convention
) {
    public static LoginResponse bearer(String token, long expiresInSeconds) {
        return new LoginResponse(token, "Bearer", expiresInSeconds);
    }
}
