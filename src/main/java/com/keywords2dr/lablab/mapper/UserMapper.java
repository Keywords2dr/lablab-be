package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.user.ProfileResponse;
import com.keywords2dr.lablab.dto.user.UpdateProfileRequest;
import com.keywords2dr.lablab.dto.user.UserResponseDTO;
import com.keywords2dr.lablab.entity.Profile;
import com.keywords2dr.lablab.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "fullName",    source = "profile.fullName")
    @Mapping(target = "phoneNumber", source = "profile.phoneNumber")
    @Mapping(target = "email",       source = "profile.email")
    @Mapping(target = "faculty",     source = "profile.faculty")
    @Mapping(target = "major",       source = "profile.major")
    @Mapping(target = "department",  source = "profile.department")
    @Mapping(target = "avatar",      source = "profile.avatar")
    ProfileResponse toProfileResponse(User user);

    @Mapping(target = "fullName",    source = "profile.fullName")
    @Mapping(target = "phoneNumber", source = "profile.phoneNumber")
    @Mapping(target = "email",       source = "profile.email")
    @Mapping(target = "faculty",     source = "profile.faculty")
    @Mapping(target = "major",       source = "profile.major")
    @Mapping(target = "department",  source = "profile.department")
    @Mapping(target = "avatar",      source = "profile.avatar")
    UserResponseDTO toUserResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "profileId",  ignore = true)
    @Mapping(target = "user",       ignore = true)
    @Mapping(target = "faculty",    ignore = true)
    @Mapping(target = "major",      ignore = true)
    @Mapping(target = "department", ignore = true)
    void updateProfileFromRequest(UpdateProfileRequest request, @MappingTarget Profile profile);
}