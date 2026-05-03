//package com.keywords2dr.lablab.config;
//
//import com.keywords2dr.lablab.entity.Profile; // Nhớ import Profile
//import com.keywords2dr.lablab.entity.User;
//import com.keywords2dr.lablab.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DataInitializer implements CommandLineRunner {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Override
//    public void run(String... args) {
//        // Khởi tạo ADMIN
//        createUser("admin", "admin123", "ADMIN", "Quản trị viên LabLab", "Khoa CNTT");
//
//        // Khởi tạo TEACHER
//        for (int i = 1; i <= 5; i++) {
//            createUser("teacher" + i, "teacher123", "TEACHER", "Giáo viên " + i, "Khoa Hóa Học");
//        }
//
//        // Khởi tạo STUDENT
//        for (int i = 1; i <= 5; i++) {
//            createUser("student" + i, "student123", "STUDENT", "Sinh viên " + i, "Khoa Sinh Học");
//        }
//    }
//
//    /**
//     * Cập nhật hàm này để nhận thêm fullName và department
//     */
//    private void createUser(String username, String password, String role, String fullName, String department) {
//        if (userRepository.findByUsername(username).isEmpty()) {
//
//            // 1. Tạo đối tượng User
//            User user = User.builder()
//                    .username(username)
//                    .password(passwordEncoder.encode(password))
//                    .role(role)
//                    .isActive(true)
//                    .build();
//
//            // 2. Tạo đối tượng Profile (Dữ liệu mẫu)
//            Profile profile = Profile.builder()
//                    .fullName(fullName)
//                    .department(department)
//                    .email(username + "@qnu.edu.vn") // Tự độnga ra email mẫu
//                    .user(user) // QUAN TRỌNG: Gắn user vào profile
//                    .build();
//
//            // 3. Gắn ngược profile vào user
//            user.setProfile(profile);
//
//            // 4. Lưu User.
//            // NHỜ CÓ cascade = CascadeType.ALL, SPRING SẼ TỰ ĐỘNG LƯU LUÔN PROFILE XUỐNG DB!
//            userRepository.save(user);
//
//            log.info(">>> Đã tạo thành công tài khoản & Profile: [{}] - Role: {}", username, role);
//        } else {
//            log.debug("--- Tài khoản [{}] đã tồn tại, bỏ qua.", username);
//        }
//    }
//}