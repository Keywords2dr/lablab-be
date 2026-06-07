# 🧪 LabLab — Hệ thống Quản lý Phòng Lab & Hóa chất

Hệ thống quản lý phòng thí nghiệm và hóa chất dành cho trường đại học, hỗ trợ quy trình mượn/trả phòng, quản lý tồn kho hóa chất, phân công giảng viên, và thông báo tự động.

---

## 🚀 Tính năng chính

### 👤 Quản lý người dùng
- Đăng nhập bằng JWT, phân quyền theo role: **ADMIN / TEACHER / STUDENT**
- Quản lý hồ sơ cá nhân, đổi mật khẩu
- Quên mật khẩu qua email OTP (5 phút hết hạn)
- Admin tạo/khóa/reset mật khẩu tài khoản

### 🏫 Quản lý Phòng Lab
- CRUD phòng Lab, kích hoạt/tạm ngưng phòng
- Phân công giảng viên phụ trách phòng (1 giảng viên / 1 phòng)
- Xem trạng thái sử dụng phòng theo thời gian thực

### 📋 Quy trình Phiếu mượn
- Sinh viên / Giảng viên tạo phiếu mượn phòng (`ROOM_ONLY`) hoặc mượn hóa chất (`CHEMICAL_ONLY`)
- Luồng duyệt: `PENDING_OWNER` → `PENDING_ADMIN` (nếu có hóa chất) → `APPROVED` → `BORROWED` → `PENDING_RETURN` → `RETURNED`
- Giảng viên duyệt/từ chối phiếu thuộc phòng mình quản lý
- Admin duyệt phiếu có hóa chất
- Kiểm tra trùng lịch phòng tự động
- Thời gian mượn tối đa 7 ngày

### 🧪 Quản lý Hóa chất & Tồn kho
- CRUD hóa chất, soft delete, khôi phục từ thùng rác
- Import hàng loạt từ file Excel (.xlsx), export danh sách
- Phân bổ / thu hồi hóa chất vào từng phòng
- Theo dõi `totalQuantity` / `lockedQuantity` / `availableQuantity`
- Chuẩn hóa dữ liệu tự động (đóng gói, đơn vị) qua từ điển học máy

### 🔔 Thông báo
- Thông báo realtime khi phiếu được duyệt, từ chối, bàn giao, trả đồ
- Nhắc nhở trước 30 phút khi phiếu sắp hết hạn (scheduler)
- Cảnh báo quá hạn định kỳ (3 tiếng / lần) và nghiêm trọng (6 tiếng / lần)
- Cảnh báo tồn kho thấp / hết hàng cho Admin

### 📊 Dashboard & Báo cáo
- Thống kê phiếu mượn theo tuần (biểu đồ 7 ngày T2–CN)
- Activity feed: nhật ký hành động gần nhất
- Trạng thái sử dụng phòng: `available / occupied / maintenance`
- Audit log toàn bộ thao tác (CREATE, UPDATE, DELETE...)

### 🤖 Chat AI (Gemini)
- Trợ lý AI tích hợp Gemini 2.5 Flash
- Trả lời câu hỏi về phòng, hóa chất, lịch mượn dựa trên dữ liệu thực tế
- Phân quyền context theo role (Admin / Teacher / Student)

---

## 🏗️ Kiến trúc hệ thống

Dự án theo chuẩn **3-Tier Architecture** kết hợp **Separation of Concerns**:

```
Client (Frontend)
      │
      ▼
Controller Layer   ← Nhận request, validate, trả response
      │
      ▼
Service Layer      ← Xử lý business logic
      │
      ▼
Repository Layer   ← Truy vấn database (Spring Data JPA)
      │
      ▼
PostgreSQL Database
```

### 📁 Cấu trúc thư mục

```text
src/main/java/com/keywords2dr/lablab/
├── config/       # Cấu hình ứng dụng (Security, CORS, WebMVC, Seeder)
├── controller/   # REST API endpoints
├── dto/          # Data Transfer Objects (Request / Response)
├── entity/       # JPA Entities ánh xạ với bảng database
├── event/        # Spring Events (NotificationEvent, Listener)
├── exception/    # Global exception handler, custom exceptions
├── mapper/       # MapStruct: Entity ↔ DTO
├── repository/   # Spring Data JPA repositories + Specification
├── security/     # JWT filter, CustomUserDetails, RateLimitInterceptor
├── service/      # Business logic (interface + impl)
└── util/         # Utilities: Scheduler, UserNameResolver...
```

---

## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Backend framework | Spring Boot 3.4.3 |
| Ngôn ngữ | Java 21 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Bảo mật | Spring Security + JWT (JJWT) |
| Object mapping | MapStruct 1.6.3 |
| Excel | Apache POI 5.5.1 |
| Email | Spring Mail (Gmail SMTP) |
| AI | Google Gemini 2.5 Flash |
| Rate limiting | Custom interceptor (ConcurrentHashMap) |
| Build tool | Maven |

---

## ⚙️ Hướng dẫn chạy dự án

### Yêu cầu
- Java 21+
- Maven 3.8+
- PostgreSQL (hoặc dùng cloud DB)

### 1. Clone repository
```bash
git clone https://github.com/<your-username>/lablab.git
cd lablab
```

### 2. Tạo file `.env` tại thư mục gốc
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:<port>/<dbname>?ssl=require
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password

JWT_SECRET=your_jwt_secret_min_32_chars

SPRING_MAIL_USERNAME=your_gmail@gmail.com
SPRING_MAIL_PASSWORD=your_app_password

SPRING_AI_GOOGLE_GENAI_API_KEY=your_gemini_api_key

FRONTEND_URL=http://localhost:5173
```

### 3. Chạy ứng dụng
```bash
mvn spring-boot:run
```

Khi khởi động lần đầu, hệ thống tự động:
- Tạo toàn bộ bảng database (Hibernate `ddl-auto=update`)
- Seed dữ liệu mẫu: 1 Admin, 10 Teacher, 10 Student, 18 Phòng Lab, 60 Hóa chất

### 4. Tài khoản mẫu

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Teacher | `teacher1` → `teacher10` | `123456` |
| Student | `student1` → `student10` | `123456` |

---

## 🗄️ Database

File SQL dump đầy đủ (schema + data mẫu) nằm tại:
```
database/database_full.sql
```

Import vào PostgreSQL:
```bash
psql -h <host> -U <username> -d <dbname> -f database/database_full.sql
```

---

## 🔐 Bảo mật

- **JWT Authentication**: Token-based, stateless
- **Rate Limiting**: Giới hạn 5 request/phút cho `/api/auth/login` và `/api/auth/forgot-password`
- **Role-based Authorization**: `@PreAuthorize` trên từng endpoint
- **Input Validation**: `@Valid` + custom validator
- **Soft Delete**: Hóa chất không bị xóa vật lý khỏi DB

---