package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.user.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface UserMapper {
    UserResponse userToUserResponse(User user);
}
