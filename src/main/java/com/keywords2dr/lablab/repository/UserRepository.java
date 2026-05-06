package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    @Query("SELECT DISTINCT u FROM User u JOIN u.profile p WHERE p.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT COUNT(p) > 0 FROM Profile p WHERE p.email = :email AND p.user.userId != :excludeUserId")
    boolean existsByEmailAndUserIdNot(@Param("email") String email, @Param("excludeUserId") UUID excludeUserId);

    @Query("SELECT COUNT(p) > 0 FROM Profile p WHERE p.phoneNumber = :phoneNumber AND p.user.userId != :excludeUserId")
    boolean existsByPhoneNumberAndUserIdNot(@Param("phoneNumber") String phoneNumber, @Param("excludeUserId") UUID excludeUserId);
}