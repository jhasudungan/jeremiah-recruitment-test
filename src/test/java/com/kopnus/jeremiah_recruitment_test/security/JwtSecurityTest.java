package com.kopnus.jeremiah_recruitment_test.security;

import com.kopnus.jeremiah_recruitment_test.config.JwtProperties;
import com.kopnus.jeremiah_recruitment_test.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class JwtSecurityTest {

    private static final String SECRET = "VzavRKfdOC06rlGfMamlFXmJP+Ep2h2HrlHEc3lTAeA=";

    private final JwtService jwtService =
            new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15)));

    private User alice() {
        User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(1L);
        Mockito.when(user.getUsername()).thenReturn("alice");
        return user;
    }

    @Test
    void generatedTokenCanBeParsedBack() {
        String token = jwtService.generateToken(alice());

        AuthenticatedUser result = jwtService.parseToken(token);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("alice");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(alice());
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived =
                new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-1)));
        String token = shortLived.generateToken(alice());

        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
