//package com.keywords2dr.lablab.config;
//
//import com.keywords2dr.lablab.entity.Profile;
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
//        // 1. Khởi tạo ADMIN (Giữ nguyên username/password)
//        createUser("admin", "admin123", "ADMIN",
//                "Nguyễn Quản Trị", "admin@qnu.edu.vn",
//                "Khoa Công nghệ thông tin", "Công nghệ thông tin", "Kỹ thuật phần mềm");
//
//        // Danh sách tên mẫu cho dữ liệu nhìn thực tế
//        String[] teacherNames = {"Trần Văn An", "Lê Thị Bình", "Phạm Văn Cường", "Hoàng Thị Dung", "Vũ Văn Em", "Đặng Thị Hoa", "Bùi Văn Giang", "Đỗ Thị Hạnh", "Hồ Văn Inh", "Ngô Thị Khanh"};
//        String[] studentNames = {"Nguyễn Thị Kiều", "Trần Văn Long", "Lê Thị Mai", "Phạm Văn Nam", "Hoàng Thị Oanh", "Vũ Văn Phong", "Đặng Thị Quỳnh", "Bùi Văn Rồng", "Đỗ Thị Sương", "Hồ Văn Tài"};
//
//        // 2. Khởi tạo 10 TEACHER (Password: 123456)
//        for (int i = 0; i < 10; i++) {
//            createUser("teacher" + (i + 1), "123456", "TEACHER",
//                    teacherNames[i],
//                    "teacher" + (i + 1) + "@qnu.edu.vn",
//                    "Khoa Khoa học Tự nhiên",
//                    "Hóa học",
//                    "Bộ môn Hóa Hữu cơ");
//        }
//
//        // 3. Khởi tạo 10 STUDENT (Password: 123456)
//        for (int i = 0; i < 10; i++) {
//            createUser("student" + (i + 1), "123456", "STUDENT",
//                    studentNames[i],
//                    "student" + (i + 1) + "@st.qnu.edu.vn",
//                    "Khoa Khoa học Tự nhiên",
//                    "Sinh học",
//                    "Bộ môn Vi sinh");
//        }
//    }
//
//    /**
//     * Hàm helper để tạo user với đầy đủ thông tin Profile (Khớp với file Profile.java)
//     */
//    private void createUser(String username, String password, String role, String fullName, String email, String faculty, String major, String department) {
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
//            // 2. Tạo đối tượng Profile chứa thông tin chi tiết
//            Profile profile = Profile.builder()
//                    .fullName(fullName)
//                    .email(email)
//                    .faculty(faculty)       // Khoa
//                    .major(major)           // Ngành
//                    .department(department) // Bộ môn
//                    .user(user)             // Gắn kết với user
//                    .build();
//
//            // 3. Gắn ngược profile vào user
//            user.setProfile(profile);
//
//            // 4. Lưu xuống Database (CascadeType.ALL sẽ tự lưu Profile)
//            userRepository.save(user);
//
//            log.info(">>> Đã tạo thành công tài khoản: [{}] - Role: {}", username, role);
//        } else {
//            log.debug("--- Tài khoản [{}] đã tồn tại, bỏ qua.", username);
//        }
//    }
//}