package com.keywords2dr.lablab.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id")
    private UUID profileId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    @JsonBackReference
    private User user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    @Email(message = "Định dạng Email không chính xác!")
    @Column(name = "email", nullable = true)
    private String email;

    @Column(name = "faculty", nullable = true)
    private String faculty;

    @Column(name = "major", nullable = true)
    private String major;

    @Column(name = "department", nullable = true)
    private String department;

    @Column(name = "avatar", length = 500)
    private String avatar;
}