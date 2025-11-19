package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends ContainersEnvironment {

    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("GivenExistingUser_WhenFindByUsername_ThenReturnsUser")
    void GivenExistingUser_WhenFindByUsername_ThenReturnsUser() {
        // given
        var username = "existing_user";
        var user = User.builder()
                .username(username)
                .password("pass")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .role(Role.WAITER)
                .build();

        userRepository.saveAndFlush(user);

        // when
        Optional<User> result = userRepository.findByUsername(username);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("GivenNoUserWithGivenUsername_WhenFindByUsername_ThenReturnsEmpty")
    void GivenNoUserWithGivenUsername_WhenFindByUsername_ThenReturnsEmpty() {
        // given
        var username = "non_existing_user";

        // when
        Optional<User> result = userRepository.findByUsername(username);

        // then
        assertThat(result).isEmpty();
    }
}
