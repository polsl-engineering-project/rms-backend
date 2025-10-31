package com.polsl.engineering.project.rms.user;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface UserMapper {
    UserResponse userToUserResponse(User user);
}
