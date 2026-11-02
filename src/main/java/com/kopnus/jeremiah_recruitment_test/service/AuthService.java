package com.kopnus.jeremiah_recruitment_test.service;

import com.kopnus.jeremiah_recruitment_test.dto.LoginRequest;
import com.kopnus.jeremiah_recruitment_test.dto.LoginResponse;
import com.kopnus.jeremiah_recruitment_test.entity.User;
import com.kopnus.jeremiah_recruitment_test.exception.InvalidCredentialsException;
import com.kopnus.jeremiah_recruitment_test.repository.UserRepository;
import com.kopnus.jeremiah_recruitment_test.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        // Used to spend the same BCrypt time when the username does not exist
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByUsername(request.username());

        String hashToCheck = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.get());
        return LoginResponse.bearer(token, jwtService.getExpiration().toSeconds());
    }
}
