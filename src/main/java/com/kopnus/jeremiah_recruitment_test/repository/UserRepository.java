package com.kopnus.jeremiah_recruitment_test.repository;

import com.kopnus.jeremiah_recruitment_test.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Used by login / JWT authentication
    Optional<User> findByUsername(String username);

}
