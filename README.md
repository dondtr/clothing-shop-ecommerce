# Clothing Shop E-Commerce

Ứng dụng web bán quần áo trực tuyến viết bằng Java (Jakarta Servlet/JSP, Hibernate/JPA, MySQL), chạy trên Apache Tomcat 11.

Đã có: nền tảng dự án (Maven, JPA, entity theo ERD sửa lần 4), cấu hình CSDL cho MySQL local và Aiven, giao diện từ template Shoppers, và chức năng **đăng ký tài khoản khách hàng có xác thực email (FR-01)**, **đăng nhập, đăng xuất (FR-02)** theo đặc tả v0.7 và sequence diagram SD04.

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
| Jakarta Standard Tag Library | API 3.0.2, cài đặt GlassFish 3.0.1 | Tomcat không kèm JSTL |
| Jakarta Mail | Eclipse Angus Mail 2.0.5 (Jakarta Mail 2.1.5) | Gửi email của đặc tả (E1 – E9) |
| JUnit Jupiter | 6.1.3 (`test`) | Kiểm thử tự động |
| H2 Database | 2.5.252 (`test`) | CSDL trong bộ nhớ, chỉ dùng khi chạy test |

Không dùng Spring, Spring Boot hay Spring Data JPA. Mọi API đều dùng namespace `jakarta.*`.

## Cấu trúc thư mục

```
clothing-shop-ecommerce/
├── .mvn/wrapper/maven-wrapper.properties   Cấu hình Maven Wrapper
├── mvnw, mvnw.cmd                          Maven Wrapper (Unix / Windows)
├── pom.xml
├── config/                                 Mẫu cấu hình local và Aiven (*.properties.example)
├── database/schema.sql                     Tạo CSDL và bảng theo ERD (sửa lần 4)
├── database/seed-dev.sql                   Dữ liệu mẫu (sản phẩm, giảm giá, đánh giá), chỉ dùng trên máy local
└── src/main/
    ├── java/com/clothingshop/
    │   ├── controller/   Servlet (trang chủ, đăng ký, xác thực email, đăng nhập, đăng xuất, cửa hàng, chi tiết sản phẩm), AppContextListener
    │   ├── service/      AccountService (FR-01, FR-02), CatalogService (FR-07, FR-08), PendingRegistrationStore, EmailService (SMTP / ghi log)
    │   ├── dao/          UserDao, ProductDao, DiscountDao, ReviewDao: truy cập dữ liệu bằng JPA EntityManager
    │   ├── dto/          AuthUser (người dùng đã đăng nhập), RegistrationForm, FlashMessage, ProductFilter, ProductSummary, ProductPage, ProductDetail
    │   ├── filter/       CsrfFilter: chống giả mạo request cho mọi form POST
    │   ├── exception/    ValidationException (lỗi theo từng ô), BusinessException
    │   ├── logging/      (để dành)
    │   ├── entity/       Entity JPA và enum theo ERD
    │   └── util/         AppConfig, JpaUtil, PasswordHasher (PBKDF2), SignedToken (HMAC), WebUtil, Money (làm tròn tiền), ViewFunctions (định dạng cho JSP)
    ├── resources/
    │   ├── META-INF/persistence.xml
    │   └── logback.xml   Log ra console, mức INFO
    └── webapp/
        ├── assets/       CSS, JS, font, ảnh của template Shoppers
        └── WEB-INF/
            ├── views/    JSP (common: header, footer; auth: đăng ký, đăng nhập; shop: cửa hàng, chi tiết sản phẩm; error)
            ├── tags/     product-card.tag: ô sản phẩm dùng lại ở nhiều trang
            ├── shop.tld  Hàm định dạng dùng trong JSP với tiền tố f (f:vnd, f:stars, f:dateTime...)
            └── web.xml
src/test/java/            Test JUnit (chạy trên H2 trong bộ nhớ)
```

Package `logging` chưa có mã nguồn; file `package-info.java` giữ lại thư mục trong Git.

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

Kết quả: `target/clothing-shop.war`. Lệnh `package` chạy luôn test; chỉ chạy test thì dùng `mvnw.cmd test`.

Test chạy trên CSDL H2 trong bộ nhớ (Hibernate tạo bảng tạm cho riêng lần chạy test), không cần MySQL và không đụng tới CSDL thật.

## Cơ sở dữ liệu

### 1. Tạo CSDL và bảng

Ứng dụng **không tự tạo, sửa hay xóa bảng** (`jakarta.persistence.schema-generation.database.action=none`). Chạy `database/schema.sql` một lần trước khi chạy ứng dụng. File này được sinh từ ERD Clothing E-Commerce (sửa lần 4); tạo CSDL `clothing_shop` với collation `utf8mb4_0900_as_ci` (không phân biệt chữ hoa/thường nhưng phân biệt dấu tiếng Việt, theo đặc tả mục 3.1).

```bash
# MySQL trên máy local
mysql -u root -p < database/schema.sql

# Nên tạo một tài khoản riêng cho ứng dụng thay vì dùng root (chạy trong mysql):
#   CREATE USER 'clothing_app'@'localhost' IDENTIFIED BY '<mật khẩu>';
#   GRANT SELECT, INSERT, UPDATE, DELETE ON clothing_shop.* TO 'clothing_app'@'localhost';

# Dữ liệu mẫu để chạy thử (chỉ trên máy local, không chạy trên Aiven; chạy một lần sau schema.sql)
mysql -u root -p --default-character-set=utf8mb4 < database/seed-dev.sql
```

`seed-dev.sql` có 7 mặt hàng đang bán và 1 mặt hàng không bán, gồm các trường hợp: đang giảm giá, giảm giá chưa bắt đầu, giảm giá đã kết thúc, hết hàng một vài dòng, hết hàng mọi dòng. Hai khách hàng mẫu chỉ để làm người viết đánh giá và không đăng nhập được (mật khẩu không phải chuỗi băm hợp lệ); muốn thử mua hàng thì tự đăng ký tài khoản.

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

### 4. Cấu hình khác

| Khóa | Bắt buộc | Ý nghĩa |
|---|---|---|
| `app.secret` | Nên có | Khóa bí mật (ít nhất 32 ký tự ngẫu nhiên) để ký link trong email. Thiếu thì mỗi lần khởi động tự sinh khóa tạm, link đã gửi hết hiệu lực khi khởi động lại |
| `app.baseUrl` | Khi triển khai | Địa chỉ gốc để tạo link trong email, ví dụ `https://shop.example.com/clothing-shop`. Thiếu thì lấy theo request (chỉ dùng khi phát triển) |
| `mail.smtp.host`, `mail.smtp.port`, `mail.smtp.username`, `mail.smtp.password`, `mail.smtp.starttls`, `mail.from` | Khi cần gửi email thật | Thiếu `mail.smtp.host` thì email **không được gửi** mà được ghi vào log (mở link xác thực từ log khi phát triển) |

## Deploy lên Tomcat 11

1. Tạo CSDL và cấu hình kết nối như mục Cơ sở dữ liệu.
2. Chép `target/clothing-shop.war` vào thư mục `webapps` của Tomcat, hoặc deploy qua Tomcat Manager.
3. Mở `http://localhost:8080/clothing-shop/` (đổi cổng theo cấu hình Tomcat).

## Chức năng đã làm

| URL | Chức năng |
|---|---|
| `/` | Trang chủ |
| `/register` | Đăng ký tài khoản khách hàng (FR-01): kiểm tra từng ô theo ERD và đặc tả (tên đăng nhập tối đa 50 ký tự, không chứa @ hay khoảng trắng; email, tên đăng nhập không trùng, không phân biệt hoa thường; mật khẩu từ 8 ký tự), gửi email E8 |
| `/register/sent` | Trang nhắc mở email xác thực |
| `/register/verify?token=...` | Link trong email E8: hiệu lực 24 giờ, dùng một lần; tạo `User` + `Customer` (hạng CASUAL) trong một giao dịch |
| `/login` | Đăng nhập bằng tên đăng nhập hoặc email (FR-02); nhân viên bị khóa không đăng nhập được |
| `/logout` | Đăng xuất (chỉ nhận POST) |
| `/shop` | Cửa hàng (FR-07): các mặt hàng đang bán, lọc theo danh mục (`category`), tên (`q`, không phân biệt hoa thường), khoảng giá sau giảm (`minPrice`, `maxPrice`), màu (`color`); 12 mặt hàng mỗi trang (`page`), mới nhất trước; nhãn "Hết hàng" khi mọi dòng hết tồn kho |
| `/product?id=...` | Chi tiết mặt hàng (FR-08): `id` là một dòng bất kỳ của mặt hàng; chọn màu + kích thước để xem ảnh và tồn kho của dòng đó; điểm trung bình và đánh giá của mọi dòng; mặt hàng không bán trả về 404 |

Ghi chú kỹ thuật:
- Đăng ký chưa xác thực được giữ trong bộ nhớ server, không ghi CSDL; mất khi khởi động lại server (Q35, Q50).
- Mật khẩu băm bằng PBKDF2-HMAC-SHA256 có muối, 600.000 vòng (có sẵn trong JDK). Link trong email ký bằng HMAC-SHA256 với `app.secret`.
- Mọi form POST có token CSRF; đăng nhập đổi session id; cookie phiên `HttpOnly`, không đưa session id lên URL.
- Thông báo sai thông tin đăng nhập luôn giống nhau, không cho biết tài khoản có tồn tại hay không.
- Mặt hàng là các dòng `Product` cùng tên (so sánh không phân biệt hoa thường). Giá sau giảm = `basePrice` trừ % của đợt giảm giá đang hiệu lực (startDate ≤ hiện tại ≤ endDate), làm tròn đến hàng nghìn, từ 500 đồng làm tròn lên (`Money`, `Product.getCurrentPrice`, `DiscountDao.findAvailable`; giỏ hàng và đặt hàng dùng lại).
- Cửa hàng nạp các dòng đang bán rồi gom và lọc theo giá trong bộ nhớ, vì lọc theo giá sau giảm phụ thuộc đợt giảm giá đang hiệu lực. Với vài nghìn dòng thì vẫn nhẹ; nếu dữ liệu lớn hơn nhiều thì chuyển việc gom nhóm và phân trang xuống SQL.
- JSP trên Tomcat 11 (EL 6.0) đọc record qua các thành phần của record, không gọi getter hay phương thức khác; giá trị nào trang cần (kể cả giá trị tính ra như `onSale`) phải là thành phần của record.

Giao diện lấy từ template **Shoppers** của Colorlib (giấy phép CC BY 3.0, phải giữ dòng ghi công ở chân trang), dựa trên bản JSP đã Việt hóa của nhóm. Các liên kết tới chức năng chưa làm (giỏ hàng, đơn hàng...) hiện mở trang 404. Ảnh sản phẩm mẫu cũng lấy từ template này.

## Kiểm tra đã chạy

- `mvnw.cmd clean package`: 34 test JUnit đạt, tạo được `target/clothing-shop.war`.
- MySQL Server 26.7.0 (một bản chạy tạm, dữ liệu riêng): `database/schema.sql` tạo đủ 16 bảng, 19 khóa ngoại; Hibernate kiểm tra ánh xạ (`hibernate.hbm2ddl.auto=validate`, chỉ dùng lúc kiểm tra) không báo lỗi.
- Tomcat 11.0.24 (một bản chạy tạm) + MySQL trên: đăng ký sai thì báo lỗi từng ô; đăng ký đúng thì chưa có tài khoản trong CSDL; mở link trong email (lấy từ log) thì tài khoản được tạo, dùng lại link bị từ chối; đăng nhập bằng email viết hoa thành công, sai mật khẩu bị từ chối; đăng xuất; POST thiếu token CSRF bị chặn (403).
- Tomcat 11.0.24 + MySQL 26.7.0 tạm, nạp `seed-dev.sql`: cửa hàng hiện 7 mặt hàng (mặt hàng không bán bị ẩn); lọc danh mục, tên có dấu, màu, khoảng giá sau giảm cho đúng kết quả; giá nhập sai báo lỗi; giá giảm 15% hiện 150.000 → 128.000; trang chi tiết hiện đánh giá (4,5/5); mặt hàng không bán và `id` không tồn tại trả về 404.
- **Chưa thử:** gửi email qua SMTP thật; kết nối Aiven.

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

## Chưa làm

- Các chức năng còn lại của đặc tả: quên mật khẩu, thông tin cá nhân, địa chỉ (FR-03 → FR-06), giỏ hàng, yêu thích, đặt hàng, thanh toán VNPAY, quản lý sản phẩm, quản trị...
- Tài khoản quản lý tạo sẵn khi triển khai (BR-24).
