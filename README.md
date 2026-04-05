# Pet Management System - Hệ thống quản lý thú cưng quy mô lớn

Hệ thống web quản lý thú cưng toàn diện, phù hợp cho cửa hàng thú cưng, phòng khám thú y, hoặc bán lại cho khách hàng.

## Tính năng

- **Quản lý thú cưng**: Thêm, sửa, xóa, tìm kiếm thú cưng
- **Phân loại**: Loại (Chó, Mèo...), giống, thông tin chi tiết
- **Lịch hẹn**: Đặt lịch khám, tiêm phòng, chăm sóc
- **Sản phẩm**: Danh mục sản phẩm, quản lý tồn kho, giá
- **Phân quyền**: Admin, Manager, Staff, Vet, Customer
- **Bảo mật**: Spring Security, mật khẩu mã hóa BCrypt

## Công nghệ

- Java 17, Spring Boot 3.2
- Spring Data JPA, Spring Security
- Thymeleaf, Bootstrap 5
- H2 (dev) / MySQL (production)

## Chạy ứng dụng

### Yêu cầu
- JDK 17+
- Maven hoặc Gradle

### Chạy với H2 (không cần cài đặt gì thêm)

```bash
./gradlew bootRun
```

Truy cập: http://localhost:8080

### Tài khoản mẫu

| Tài khoản | Mật khẩu | Vai trò |
|-----------|----------|---------|
| admin     | admin123 | Quản trị viên |
| manager   | manager123 | Quản lý |
| customer  | customer123 | Khách hàng |

### H2 Console

Truy cập: http://localhost:8080/h2-console  
- JDBC URL: `jdbc:h2:mem:petdb`
- Username: `sa`
- Password: (để trống)

## Chuyển sang MySQL

Sửa `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pet_management?useUnicode=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

Tạo database:
```sql
CREATE DATABASE pet_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## Cấu trúc dự án

```
src/main/java/com/db7/j2ee_quanlythucung/
├── config/          # Cấu hình Security
├── controller/      # Web Controllers
├── entity/          # JPA Entities
├── repository/      # Spring Data JPA
├── security/        # UserDetails, CustomUserDetailsService
├── service/         # Business logic
└── J2EeQuanLyThuCungApplication.java
```

## Triển khai production

1. Chuyển sang MySQL/PostgreSQL
2. Đặt `spring.jpa.hibernate.ddl-auto=update` hoặc dùng Flyway/Liquibase
3. Cấu hình HTTPS
4. Đổi mật khẩu admin
5. Cấu hình mail (nếu dùng gửi email)

## Bản quyền

© 2025 Pet Management System
