package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID profileId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100)
    private String fullName;

    @Column(length = 10)
    private String phoneNumber;

    @NotBlank(message = "Email không được để trống!")
    @Email(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
            message = "Định dạng Email không chính xác (Ví dụ hợp lệ: name@domain.com)!"
    )
    private String email;

    @Column(length = 100)
    private String faculty;

    @Column(length = 100)
    private String major;

    @Column(length = 100)
    private String department;

    @Column(length = 500)
    private String avatar;
}