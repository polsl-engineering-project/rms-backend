package com.polsl.engineering.project.rms.user;

import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class UserMapperTest {

    UserMapper underTest = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("Given User, When userToUserResponse, Then returns correctly maps")
    void GivenUser_WhenUserToUserResponse_ThenReturnsCorrectlyMaps() {
        // Given
        var user = Instancio.create(User.class);

        // When
        var result = underTest.userToUserResponse(user);

        // Then
        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.username()).isEqualTo(user.getUsername());
        assertThat(result.firstName()).isEqualTo(user.getFirstName());
        assertThat(result.lastName()).isEqualTo(user.getLastName());
        assertThat(result.phoneNumber()).isEqualTo(user.getPhoneNumber());
        assertThat(result.role()).isEqualTo(user.getRole());
    }


}