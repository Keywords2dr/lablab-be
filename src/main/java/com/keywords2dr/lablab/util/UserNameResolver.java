package com.keywords2dr.lablab.util;

import com.keywords2dr.lablab.entity.User;

public final class UserNameResolver {

    private UserNameResolver() {}


    public static String resolve(User user) {
        if (user == null) return null;
        if (user.getProfile() != null
                && user.getProfile().getFullName() != null) {
            return user.getProfile().getFullName();
        }
        return user.getUsername();
    }
}