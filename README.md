# Clothing Shop E-Commerce

Ứng dụng web bán quần áo trực tuyến viết bằng Java (Jakarta Servlet/JSP, Hibernate/JPA, MySQL), chạy trên Apache Tomcat 11.

Đây là **nền tảng ban đầu** của dự án: cấu hình Maven, thư viện, cấu trúc thư mục, cấu hình JPA và các entity theo ERD. Chưa có controller, service, DAO hay trang JSP nào.

## Công nghệ

| Thành phần | Phiên bản | Ghi chú |
|---|---|---|
| Java | 17 | Biên dịch với `maven.compiler.release=17`; JDK 17 trở lên đều build được |
| Jakarta Servlet API | 6.1.0 (`provided`) | Tomcat 11.0.x cài đặt Servlet 6.1, JSP 4.0, EL 6.0 |
| Hibernate ORM | 7.4.12.Final | Nhánh ổn định mới nhất; tương thích Java 17, Jakarta Persistence 3.2. Phiên bản được quản lý bằng BOM `hibernate-platform` |
| Jakarta Persistence API | 3.2.0 | Phiên bản do BOM của Hibernate quản lý |
| HikariCP | 7.0.2 (qua `hibernate-hikaricp`) | Pool kết nối; phiên bản do BOM của Hibernate quản lý |
| MySQL Connector/J | 26.7.0 | Hỗ trợ MySQL Server 8.4 trở lên; máy phát triển dùng MySQL Server 26.7. Nếu dùng MySQL 8.0 thì đổi sang 9.7.0 |
| SLF4J API | 2.0.20 | |
| Logback | 1.6.5 | Backend log duy nhất. Hibernate ghi log qua JBoss Logging, tự nhận SLF4J/Logback |
| Maven (qua Wrapper) | 3.9.16 | |
| Apache Tomcat | 11.0.x | |

Không dùng Spring, Spring Boot hay Spring Data JPA. Mọi API đều dùng namespace `jakarta.*`.

## Cấu trúc thư mục

```
clothing-shop-ecommerce/
├── .mvn/wrapper/maven-wrapper.properties   Cấu hình Maven Wrapper
├── mvnw, mvnw.cmd                          Maven Wrapper (Unix / Windows)
├── pom.xml
├── config/                                 Mẫu cấu hình local và Aiven (*.properties.example)
├── database/schema.sql                     Tạo CSDL và bảng theo ERD (sửa lần 4)
└── src/main/
    ├── java/com/clothingshop/
    │   ├── controller/   Servlet nhận request, kiểm tra đầu vào, gọi service, chọn view   (để dành)
    │   ├── service/      Nghiệp vụ và phạm vi giao dịch                                   (để dành)
    │   ├── dao/          Truy cập dữ liệu bằng JPA EntityManager                          (để dành)
    │   ├── dto/          Đối tượng truyền dữ liệu giữa các tầng                           (để dành)
    │   ├── filter/       Xác thực, phân quyền, xử lý request chung                        (để dành)
    │   ├── exception/    Exception riêng của ứng dụng                                     (để dành)
    │   ├── logging/      Thành phần liên quan đến log                                     (để dành)
    │   ├── entity/       Entity JPA và enum theo ERD
    │   └── util/         AppConfig (đọc cấu hình), JpaUtil (EntityManagerFactory, giao dịch)
    ├── resources/
    │   ├── META-INF/persistence.xml
    │   └── logback.xml   Log ra console, mức INFO
    └── webapp/
        ├── assets/css, assets/js, assets/images   (để dành)
        └── WEB-INF/
            ├── views/    JSP đặt ở đây để không truy cập trực tiếp bằng URL được   (để dành)
            └── web.xml
```

Các package và thư mục ghi "để dành" chưa có mã nguồn. Mỗi package có một file `package-info.java` mô tả trách nhiệm của package; thư mục web có file `.gitkeep`. Mục đích là để Git giữ lại thư mục rỗng.

## Yêu cầu

- JDK 17 trở lên. Nếu chưa đặt `JAVA_HOME` thì lệnh `java` phải có trong PATH.
- Có mạng ở lần build đầu tiên để Maven Wrapper tải Maven 3.9.16 và các thư viện.
- Không cần cài Maven.

## Build

```bash
# Windows
mvnw.cmd clean package

# Linux / macOS
./mvnw clean package
```

Kết quả: `target/clothing-shop.war`.

## Cơ sở dữ liệu

### 1. Tạo CSDL và bảng

Ứng dụng **không tự tạo, sửa hay xóa bảng** (`jakarta.persistence.schema-generation.database.action=none`). Chạy `database/schema.sql` một lần trước khi chạy ứng dụng. File này được sinh từ ERD Clothing E-Commerce (sửa lần 4); tạo CSDL `clothing_shop` với collation `utf8mb4_0900_as_ci` (không phân biệt chữ hoa/thường nhưng phân biệt dấu tiếng Việt, theo đặc tả mục 3.1).

```bash
# MySQL trên máy local
mysql -u root -p < database/schema.sql

# Nên tạo một tài khoản riêng cho ứng dụng thay vì dùng root (chạy trong mysql):
#   CREATE USER 'clothing_app'@'localhost' IDENTIFIED BY '<mật khẩu>';
#   GRANT SELECT, INSERT, UPDATE, DELETE ON clothing_shop.* TO 'clothing_app'@'localhost';
```

### 2. Cấu hình kết nối

Thông tin kết nối **không nằm trong mã nguồn**. `com.clothingshop.util.AppConfig` đọc mỗi khóa theo thứ tự:

1. biến môi trường, ví dụ khóa `db.url` → `CLOTHINGSHOP_DB_URL`;
2. system property của JVM, ví dụ `-Dclothingshop.db.url=...`;
3. file cấu hình: đường dẫn trong biến `CLOTHINGSHOP_CONFIG`, hoặc mặc định `~/.clothingshop/clothingshop.properties` nếu file tồn tại.

| Khóa | Bắt buộc | Ý nghĩa |
|---|---|---|
| `db.url` | Có | JDBC URL, ví dụ `jdbc:mysql://localhost:3306/clothing_shop` |
| `db.username` | Có | Tài khoản MySQL |
| `db.password` | Không | Mật khẩu (mặc định rỗng) |
| `db.pool.maxSize` | Không | Số kết nối tối đa của HikariCP (mặc định 10) |

Mẫu cấu hình (chép ra ngoài repo rồi điền giá trị; file đã điền **không commit**, `.gitignore` đã chặn `config/*.properties`):

- `config/clothingshop.properties.example`: MySQL trên máy local.
- `config/clothingshop-aiven.properties.example`: MySQL trên Aiven.

Tomcat chạy dưới dạng Windows service không nhận biến môi trường của người dùng. Khi đó thêm `-Dclothingshop.config=C:\duong\dan\clothingshop.properties` trong `Tomcat11w.exe` (tab Java, mục Java Options).

### 3. Chuyển sang Aiven

1. Tạo MySQL service trên Aiven, lấy Host, Port, User (`avnadmin`), Password và `ca.pem` ở trang Overview. Kiểm tra phiên bản MySQL của service: Connector/J 26.7.0 hỗ trợ MySQL Server từ 8.4; nếu service là 8.0 thì đổi `mysql-connector-j` về 9.7.0.
2. Tạo bảng bằng `database/schema.sql` (lệnh có trong file). **Không** dump CSDL trên Windows rồi nhập vào Aiven: MySQL trên Windows lưu tên bảng bằng chữ thường (`order`, `user`), còn Aiven chạy Linux phân biệt hoa thường nên Hibernate sẽ không tìm thấy bảng `Order`, `User`.
3. Điền `config/clothingshop-aiven.properties.example` (Aiven bắt buộc SSL; file mẫu có cách tạo truststore từ `ca.pem` để dùng `sslMode=VERIFY_CA`).
4. Aiven bật `sql_require_primary_key`; mọi bảng trong ERD đều có khóa chính nên không bị ảnh hưởng.

### Kiểm tra đã chạy

Trên MySQL Server 26.7.0: `schema.sql` tạo đủ 16 bảng, 19 khóa ngoại; Hibernate kiểm tra ánh xạ (`hibernate.hbm2ddl.auto=validate`, chỉ dùng lúc kiểm tra) không báo lỗi. Chưa thử kết nối Aiven.

## Deploy lên Tomcat 11

1. Tạo CSDL và cấu hình kết nối như mục Cơ sở dữ liệu.
2. Chép `target/clothing-shop.war` vào thư mục `webapps` của Tomcat, hoặc deploy qua Tomcat Manager.
3. Ứng dụng chạy ở đường dẫn `/clothing-shop`.

Giai đoạn này chưa có trang nào để mở. Việc deploy và kết nối CSDL **chưa được chạy thử**.

## Mô hình entity

Entity bám theo ERD Clothing E-Commerce (sửa lần 4): 14 entity, 2 lớp nhúng (`OrderDetailId`, `VoucherPolicy`), 12 enum. Các quyết định ánh xạ:

- **Biến thể sản phẩm:** theo ERD, mỗi dòng `Product` là một biến thể (màu + kích thước) có tồn kho riêng. Các dòng cùng `name` là một mặt hàng; (`name`, `color`, `size`) là duy nhất.
- **User, Customer, Employee:** kế thừa `JOINED`. Mỗi tài khoản là đúng một khách hàng hoặc một nhân viên, không dùng chung cho cả hai nghiệp vụ.
- **VoucherPolicy:** lớp nhúng của `Voucher`, các cột lưu ở bảng phụ `VoucherPolicy` (cùng khóa `voucherCode`).
- **WishlistItem:** bảng nối của quan hệ `@ManyToMany` giữa `Wishlist` và `Product`, không có lớp entity riêng.
- **Quan hệ:**
  - Mọi `@ManyToOne` / `@OneToOne` đều `LAZY`.
  - Không đặt chiều ngược cho các quan hệ one-to-one (Payment, DeliveryIssue, ReturnRequest → Order).
  - Cascade chỉ dùng cho quan hệ thành phần: `Order.details`, `Customer.addresses`.
- **Khóa lạc quan:** 12 entity gốc có `@Version private Long version` (cột `version BIGINT`). Hibernate tăng version mỗi lần cập nhật và ném `OptimisticLockException` nếu bản ghi đã bị người khác lưu trước. Customer, Employee dùng version của User; VoucherPolicy dùng version của Voucher. Câu lệnh UPDATE hàng loạt phải tự tăng `version`.
- **Kiểm tra dữ liệu:** các ràng buộc CHECK của ERD (số lượng > 0, rating 1–5, tồn kho ≥ 0) sẽ do tầng service kiểm tra.

## Chưa làm (theo phạm vi giai đoạn khởi tạo)

- Controller, service, DAO, các trang JSP; đăng nhập, giỏ hàng, đặt hàng, thanh toán VNPAY, quản lý tồn kho.
- Dữ liệu mẫu; tài khoản quản lý tạo sẵn khi triển khai (BR-24).
- Đóng `EntityManagerFactory` khi ứng dụng dừng (gọi `JpaUtil.close()` từ một `ServletContextListener` khi làm tầng DAO).
- JSTL: thêm khi làm trang JSP đầu tiên.
- Kiểm thử tự động.
