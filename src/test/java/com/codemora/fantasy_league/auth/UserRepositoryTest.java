package com.codemora.fantasy_league.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsernameReturnsSavedUser() {
        userRepository.save(User.builder()
                .username("alice")
                .passwordHash("hashed")
                .role(Role.USER)
                .build());

        assertThat(userRepository.findByUsername("alice")).isPresent();
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
        assertThat(userRepository.existsByUsername("alice")).isTrue();
    }

    @Test
    void usernameMustBeUnique() {
        userRepository.saveAndFlush(User.builder()
                .username("alice")
                .passwordHash("hashed")
                .role(Role.USER)
                .build());

        assertThatThrownBy(() -> userRepository.saveAndFlush(User.builder()
                .username("alice")
                .passwordHash("different-hash")
                .role(Role.ADMIN)
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
