package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest extends ContainersEnvironment {

    @Autowired
    UserRepository underTest;

    @Autowired
    TestEntityManager em;

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
    }

    @Test
    @DisplayName("Given id of user that not exists, When updateById, Then returns zero")
    void GivenIdOfUserThatNotExists_WhenUpdateById_ThenReturnsZero() {
        // Given
        User user = createNotPersistedUser();

        em.persistAndFlush(user);

        var id = UUID.randomUUID();
        var newUsername = "newusername";
        var newFirstName = "NewFirstName";
        var newLastName = "NewLastName";
        var newPhoneNumber = "123456789";

        // When
        int updatedCount = underTest.updateById(
                id,
                newUsername,
                newFirstName,
                newLastName,
                newPhoneNumber
        );

        // Then
        assertThat(updatedCount).isZero();
    }

    @Test
    @DisplayName("Given id of user that exists, When updateById, Then returns one and updates user")
    void GivenIdOfUserThatExists_WhenUpdateById_ThenReturnsOneAndUpdatesUser() {
        // Given
        User user = createNotPersistedUser();
        User otherUser = createNotPersistedUser();

        em.persist(otherUser);
        em.persistAndFlush(user);

        var id = user.getId();
        var newUsername = "newusername";
        var newFirstName = "NewFirstName";
        var newLastName = "NewLastName";
        var newPhoneNumber = "123456789";

        // When
        int updatedCount = underTest.updateById(
                id,
                newUsername,
                newFirstName,
                newLastName,
                newPhoneNumber
        );

        // Then
        assertThat(updatedCount).isOne();

        var updatedUser = em.find(User.class, id);

        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser)
                .usingRecursiveComparison()
                .isEqualTo(user);
    }

    @Test
    @DisplayName("Given id of user that not exists, When deleteUserById, Then returns zero")
    void GivenIdOfUserThatNotExists_WhenDeleteUserById_ThenReturnsZero() {
        // Given
        var id = UUID.randomUUID();

        var otherUser = createNotPersistedUser();
        em.persistAndFlush(otherUser);

        // When
        int deletedCount = underTest.deleteUserById(id);

        // Then
        assertThat(deletedCount).isZero();
    }

    private static User createNotPersistedUser() {
        return Instancio.create(User.class)
                .toBuilder()
                .id(null)
                .createdAt(null)
                .updatedAt(null)
                .build();
    }

    @Test
    @DisplayName("Given id of user that exists, When deleteUserById, Then returns one")
    void GivenIdOfUserThatExists_WhenDeleteUserById_ThenReturnsOne() {
        // Given
        var user = createNotPersistedUser();
        var otherUser = createNotPersistedUser();

        em.persist(otherUser);
        em.persistAndFlush(user);

        var id = user.getId();


        // When
        int deletedCount = underTest.deleteUserById(id);

        // Then
        assertThat(deletedCount).isOne();
    }

    @Test
    @DisplayName("Given username of non-existing user, When existsByUsername, Then returns false")
    void GivenUsernameOfNonExistingUser_WhenExistsByUsername_ThenReturnsFalse() {
        // Given
        var user = createNotPersistedUser();
        em.persistAndFlush(user);

        var username = "nonexistingusername";

        // When
        boolean exists = underTest.existsByUsername(username);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Given username of existing user, When existsByUsername, Then returns true")
    void GivenUsernameOfExistingUser_WhenExistsByUsername_ThenReturnsTrue() {
        // Given
        var user = createNotPersistedUser();
        var otherUser = createNotPersistedUser();

        em.persist(otherUser);
        em.persistAndFlush(user);

        var username = user.getUsername();

        // When
        boolean exists = underTest.existsByUsername(username);

        // Then
        assertThat(exists).isTrue();
    }

}