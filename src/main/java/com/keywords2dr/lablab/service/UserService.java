package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.user.*;

public interface UserService {

    void changePassword(ChangePasswordRequest request);

    ProfileResponse getMyProfile();

    ProfileResponse updateMyProfile(UpdateProfileRequest request);
}