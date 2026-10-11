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
    │   ├── controller/   Servlet (trang chủ, đăng ký, xác thực email, đăng nhập, đăng xuất, cửa hàng, chi tiết sản phẩm, giỏ hàng, yêu thích), AppContextListener
    │   ├── service/      AccountService (FR-01, FR-02), CatalogService (FR-07, FR-08), CartService + Cart + CartStore (FR-09), WishlistService (FR-10), PendingRegistrationStore, EmailService (SMTP / ghi log)
    │   ├── dao/          UserDao, ProductDao, DiscountDao, ReviewDao, WishlistDao: truy cập dữ liệu bằng JPA EntityManager
    │   ├── dto/          AuthUser (người dùng đã đăng nhập), RegistrationForm, FlashMessage, ProductFilter, ProductSummary, ProductPage, ProductDetail, CartView, WishlistEntry
    │   ├── filter/       CsrfFilter: chống giả mạo request cho mọi form POST; CustomerAreaFilter: trang chỉ dành cho khách hàng đã đăng nhập
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
            ├── views/    JSP (common: header, footer; auth: đăng ký, đăng nhập; shop: cửa hàng, chi tiết sản phẩm; cart: giỏ hàng; account: yêu thích; error)
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

### 4. Tài khoản quản lý

Hệ thống có đúng một tài khoản quản lý, tạo sẵn khi triển khai (BR-24). Khi ứng dụng khởi động mà chưa có tài khoản quản lý, nó tạo tài khoản từ cấu hình (cùng nơi với `db.*`, mật khẩu không nằm trong mã nguồn):

| Khóa | Bắt buộc | Ý nghĩa |
|---|---|---|
| `admin.userName`, `admin.email`, `admin.password`, `admin.fullName`, `admin.phone` | Có | Thông tin tài khoản quản lý; mật khẩu từ 8 ký tự |
| `admin.gender`, `admin.dob` | Không | Mặc định `OTHER` và `1990-01-01`; quản lý tự sửa ở trang Thông tin cá nhân |

Đã có tài khoản quản lý thì các khóa này không còn tác dụng; nên xóa `admin.password` khỏi cấu hình sau lần khởi động đầu. Quản lý tạo tài khoản nhân viên trong khu quản lý.

### 5. VNPAY

Thanh toán VNPAY (FR-13) chỉ bật khi có đủ `vnpay.tmnCode` và `vnpay.hashSecret` (lấy ở trang quản trị merchant của VNPAY; môi trường thử là sandbox). Thiếu thì trang đặt hàng chỉ cho chọn COD.

| Khóa | Bắt buộc | Ý nghĩa |
|---|---|---|
| `vnpay.tmnCode`, `vnpay.hashSecret` | Có (để bật VNPAY) | Mã website và khóa bí mật để ký HMAC-SHA512 |
| `vnpay.payUrl`, `vnpay.apiUrl` | Không | Mặc định là sandbox: `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`, `https://sandbox.vnpayment.vn/merchant_webapi/api/transaction` |
| `vnpay.serverIp` | Không | IP máy chủ cửa hàng gửi kèm khi gọi API truy vấn, hoàn tiền (mặc định 127.0.0.1) |
| `vnpay.paymentTimeoutMinutes` | Không | Hạn thanh toán của đơn chờ, mặc định 15 phút (Q37) |
| `vnpay.processReturnUrl` | Không | Mặc định `false`: trang kết quả chỉ hiển thị trạng thái, IPN quyết định kết quả (FR-13). Khi chạy trên máy cá nhân mà VNPAY không gọi được IPN tới, đặt `true` để trang kết quả cũng áp dụng kết quả có chữ ký VNPAY gửi về; nếu không, đơn chờ chỉ được xác nhận khi J7 truy vấn (sau hạn thanh toán) |

Đăng ký IPN URL `https://<tên miền>/clothing-shop/payment/vnpay-ipn` trong trang merchant của VNPAY; Return URL do ứng dụng gửi theo `app.baseUrl` (`…/payment/vnpay-return`).

### 6. Cấu hình khác

| Khóa | Bắt buộc | Ý nghĩa |
|---|---|---|
| `app.secret` | Nên có | Khóa bí mật (ít nhất 32 ký tự ngẫu nhiên) để ký link trong email. Thiếu thì mỗi lần khởi động tự sinh khóa tạm, link đã gửi hết hiệu lực khi khởi động lại |
| `app.baseUrl` | Khi triển khai | Địa chỉ gốc để tạo link trong email, ví dụ `https://shop.example.com/clothing-shop`. Thiếu thì lấy theo request (chỉ dùng khi phát triển) |
| `shop.shippingFee` | Không | Phí vận chuyển mỗi đơn, mặc định 20000 (BR-03) |
| `scheduler.enabled` | Không | Mặc định `true`: chạy tác vụ tự động J1 – J7. Đặt `false` trên máy chủ thứ hai nếu chạy nhiều máy chủ |
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
| `/forgot-password` | Quên mật khẩu (FR-03): luôn báo "Nếu email tồn tại, hệ thống đã gửi hướng dẫn"; email E9 chứa link hiệu lực 15 phút |
| `/reset-password?token=...` | Đặt mật khẩu mới từ link E9; link có chữ ký gồm mã người dùng và dấu vết mật khẩu hiện tại nên hết hiệu lực khi mật khẩu đã đổi, không lưu CSDL |
| `/account/profile` | Thông tin cá nhân (FR-04), cho mọi tài khoản: sửa họ tên, giới tính, ngày sinh, số điện thoại; không sửa tên đăng nhập, email |
| `/account/password` | Đổi mật khẩu (FR-05): phải nhập đúng mật khẩu hiện tại |
| `/staff` | Khu quản lý (nhân viên và quản lý): việc đang chờ (đơn chờ xác nhận, chờ bàn giao, sự cố giao hàng, yêu cầu trả hàng) và các đơn mới nhất. Nhân viên đăng nhập được chuyển thẳng tới đây |
| `/staff/customers` | Tra cứu khách hàng theo tên, email, số điện thoại (FR-42); trang chi tiết có thông tin, hạng, tổng đã thanh toán, lịch sử đơn, yêu cầu trả hàng. Quản lý nâng hạng (chỉ lên hạng cao hơn, tắt tự hạ hạng) và bật/tắt tự hạ hạng (bật lại thì xét hạng ngay) (FR-28) |
| `/admin/employees` | Quản lý tạo tài khoản nhân viên STAFF (FR-40), khóa, mở khóa (FR-41); không khóa được tài khoản quản lý |
| `/account/addresses` | Sổ địa chỉ (FR-06): thêm, sửa, đặt mặc định, xóa; địa chỉ đầu tiên là mặc định; xóa địa chỉ mặc định phải chọn địa chỉ khác hoặc tạo địa chỉ mới làm mặc định trong cùng giao dịch (Q62, SD13); chuỗi "số nhà, phường/xã, tỉnh/thành phố" không quá 255 ký tự |
| `/shop` | Cửa hàng (FR-07): các mặt hàng đang bán, lọc theo danh mục (`category`), tên (`q`, không phân biệt hoa thường), khoảng giá sau giảm (`minPrice`, `maxPrice`), màu (`color`); 12 mặt hàng mỗi trang (`page`), mới nhất trước; nhãn "Hết hàng" khi mọi dòng hết tồn kho |
| `/product?id=...` | Chi tiết mặt hàng (FR-08): `id` là một dòng bất kỳ của mặt hàng; chọn màu + kích thước để xem ảnh và tồn kho của dòng đó; điểm trung bình và đánh giá của mọi dòng; mặt hàng không bán trả về 404. Khách hàng đã đăng nhập có nút "Thêm vào giỏ hàng" (dòng hết hàng không thêm được) và "Thêm vào danh sách yêu thích" |
| `/cart` | Giỏ hàng (FR-09): thêm từ trang chi tiết (`POST /cart/add`), đổi số lượng (`POST /cart/update`), xóa dòng (`POST /cart/remove`); đơn giá theo giá sau giảm hiện tại; dòng ngừng bán, hết hàng hoặc vượt tồn kho được đánh dấu và không tính vào tổng |
| `/checkout` | Đặt hàng (FR-12, FR-14, FR-15): chọn dòng trong giỏ, địa chỉ đã lưu (chọn sẵn mặc định) hoặc địa chỉ mới (được lưu vào sổ địa chỉ), ghi chú tối đa 500 ký tự, mã voucher, phương thức thanh toán; xem trước tiền sản phẩm, tiền giảm voucher, phí vận chuyển, tổng tiền; "Xác nhận mua hàng" tạo đơn COD |
| `/payment/vnpay-ipn` | VNPAY gọi để báo kết quả (FR-13 bước 4): kiểm tra chữ ký, mã đơn, số tiền; thành công thì thanh toán PAID, xóa các dòng đã đặt khỏi giỏ, xét hạng; thất bại thì xóa đơn chờ, trả tồn kho và lượt voucher; nhận lại kết quả đã xử lý thì không đổi gì |
| `/payment/vnpay-return` | Trang khách quay về từ VNPAY: hiển thị "Giao dịch thành công", "Đang xử lý" (tự kiểm tra lại) hoặc "Thanh toán không thành công" theo trạng thái thanh toán |
| `/orders`, `/orders/detail?id=...` | Đơn hàng của khách (FR-16): mới nhất trước; chi tiết có dòng hàng (đơn giá = thành tiền / số lượng), địa chỉ, người nhận, thanh toán, trạng thái, sự cố giao hàng, yêu cầu trả hàng; nút Hủy đơn (FR-17, khi chưa giao thành công), Đã nhận hàng (FR-19, khi đang giao) |
| `/order-action?token=...` | Nút trong email E1, E2 (FR-19, FR-20): không cần đăng nhập, không hết hạn, có chữ ký; "Đã nhận hàng" hoàn tất đơn đang giao, "Chưa thấy hàng đâu" ghi nhận sự cố; đơn không còn đang giao thì chỉ hiển thị trạng thái (Q24) |
| `/staff/orders` | Đơn hàng cho nhân viên, lọc theo trạng thái; chi tiết đơn có nút Xác nhận (PENDING → CONFIRMED), Bàn giao vận chuyển (→ SHIPPING, ghi shippedAt, gửi E1) (FR-18), Giao không thành công – hủy đơn (FR-23) |
| `/staff/delivery-issues` | Báo "Chưa thấy hàng đâu" đang chờ (FR-42 bước 3); xử lý bằng Giao lại hàng (trừ tồn kho thêm một lần, chỉ khi đủ hàng) hoặc Đã giải quyết, nhập lý do và cách giải quyết để gửi trong email E3/E4 (FR-21) |
| `/vouchers` | Voucher khách hàng dùng được ngay (FR-39): còn lượt, đang hiệu lực, dành cho mọi khách hàng hoặc đúng hạng |
| `/wishlist` | Danh sách yêu thích (FR-10): thêm, bỏ từ trang chi tiết (`POST /wishlist/add`, `/wishlist/remove`); lần thêm đầu tạo `Wishlist`; lưu dòng khách đang xem nhưng mỗi mặt hàng chỉ một lần; mặt hàng không còn bán vẫn nằm trong danh sách, được đánh dấu và không mở trang mua |

Ghi chú kỹ thuật:
- Đăng ký chưa xác thực được giữ trong bộ nhớ server, không ghi CSDL; mất khi khởi động lại server (Q35, Q50).
- Mật khẩu băm bằng PBKDF2-HMAC-SHA256 có muối, 600.000 vòng (có sẵn trong JDK). Link trong email ký bằng HMAC-SHA256 với `app.secret`.
- Mọi form POST có token CSRF; đăng nhập đổi session id; cookie phiên `HttpOnly`, không đưa session id lên URL.
- Thông báo sai thông tin đăng nhập luôn giống nhau, không cho biết tài khoản có tồn tại hay không.
- Mặt hàng là các dòng `Product` cùng tên (so sánh không phân biệt hoa thường). Giá sau giảm = `basePrice` trừ % của đợt giảm giá đang hiệu lực (startDate ≤ hiện tại ≤ endDate), làm tròn đến hàng nghìn, từ 500 đồng làm tròn lên (`Money`, `Product.getCurrentPrice`, `DiscountDao.findAvailable`; giỏ hàng và đặt hàng dùng lại).
- Cửa hàng nạp các dòng đang bán rồi gom và lọc theo giá trong bộ nhớ, vì lọc theo giá sau giảm phụ thuộc đợt giảm giá đang hiệu lực. Với vài nghìn dòng thì vẫn nhẹ; nếu dữ liệu lớn hơn nhiều thì chuyển việc gom nhóm và phân trang xuống SQL.
- Giỏ hàng nằm trong bộ nhớ server theo userId (Q47): mọi trình duyệt đang đăng nhập cùng tài khoản thấy chung một giỏ; mất khi khởi động lại server; giỏ không dùng 30 ngày bị dọn. Mỗi thay đổi trên giỏ của một khách hàng chạy tuần tự (`ConcurrentHashMap.compute`) và áp dụng trên bản sao, lỗi thì giỏ giữ nguyên. `CartStore` là interface để sau này thay bằng Redis (Q59) mà không sửa `CartService`.
- Số lượng trong giỏ không vượt tồn kho lúc thêm/sửa; tồn kho có thể giảm sau đó nên bước đặt hàng (FR-12) phải kiểm tra lại trong giao dịch.
- `CustomerAreaFilter` chặn `/cart`, `/wishlist`, `/checkout`, `/orders`, `/account/*`: chưa đăng nhập thì chuyển sang trang đăng nhập rồi quay lại (BR-25); tài khoản nhân viên nhận 403 (BR-27). Chức năng mới thuộc các đường dẫn này không cần tự kiểm tra đăng nhập nữa.
- Hai trình duyệt cùng sửa danh sách yêu thích: Hibernate tăng `Wishlist.version` mỗi lần thêm, bỏ mặt hàng, nên thay đổi dựa trên bản cũ bị từ chối với thông báo "Dữ liệu đã được người khác thay đổi, vui lòng tải lại" (đặc tả mục 3.1).
- Khóa lạc quan: `JpaUtil.inTransaction` đổi lỗi kiểm tra version thành `ConflictException` ("Dữ liệu đã được người khác thay đổi, vui lòng tải lại", mục 3.1). Form sửa dữ liệu gửi kèm `version` lúc mở form; service so với version hiện tại (`ConflictException.check`) nên lưu từ form cũ bị từ chối.
- Email thông báo (E1 – E7, E9) gửi qua `AsyncEmailService` trên luồng riêng: máy chủ mail chậm hoặc lỗi không làm chậm thao tác và không hoàn tác thay đổi đã lưu; lỗi được ghi log. Nội dung các email ở `service/Emails.java`. Email xác thực đăng ký (E8) vẫn gửi trực tiếp để báo khách khi không gửi được.
- `SignedInFilter` chặn `/account/profile`, `/account/password` (mọi tài khoản đã đăng nhập); `CustomerAreaFilter` chặn `/account/addresses`, `/vouchers` và các trang mua hàng.
- `StaffAreaFilter`: `/staff/*` cho nhân viên và quản lý, `/admin/*` chỉ cho quản lý; trạng thái khóa được đọc lại ở mỗi request nên nhân viên bị khóa mất quyền ngay (BR-18). Khách hàng nhận 403.
- Xét hạng (FR-27) là `CustomerService.evaluateLevel`, chạy trong giao dịch của thao tác làm thanh toán chuyển sang PAID hoặc REFUND: tổng = Σ `Order.totalAmount` của các đơn có thanh toán PAID; nâng hạng ngay, chỉ hạ khi `autoDowngrade` bật; hệ thống tự nâng lên LOYAL thì bật lại `autoDowngrade` (Q31).
- Khi Tomcat dừng hoặc deploy lại ứng dụng, `AppContextListener` tắt luồng dọn kết nối của driver MySQL và gỡ driver JDBC để Tomcat giải phóng ứng dụng cũ.
- Đặt hàng tính lại mọi thứ trong một giao dịch (Order.place()): dòng còn bán, tồn kho trừ bằng `UPDATE … SET stockQuantity = stockQuantity − n, version = version + 1 WHERE stockQuantity ≥ n` theo thứ tự id (nhiều khách đặt cùng lúc không bán vượt, không khóa chéo nhau), voucher kiểm tra lại và trừ một lượt bằng câu lệnh có điều kiện, rồi tạo `Order`, `OrderDetail` (subtotal = giá sau giảm × số lượng), `Payment` (COD, UNPAID). Lỗi ở bước nào thì không có gì được lưu và khách nhận thông báo cụ thể.
- Đơn được dựng từ giỏ lưu trên server, không tin dữ liệu trình duyệt gửi (mục 3.4). Trang đặt hàng gửi kèm tổng tiền khách đã xem; tổng tính lúc tạo đơn khác (giá, giảm giá, voucher vừa đổi) thì không tạo đơn và hiện tổng mới để khách xác nhận lại.
- Tiền giảm voucher (BR-07): phần trăm thì làm tròn đến hàng nghìn rồi giới hạn bởi `maxDiscount`; số tiền cố định thì bằng giá trị voucher; không vượt tiền sản phẩm. Phí vận chuyển 20.000 ₫ (đổi được bằng `shop.shippingFee`).
- Lượt voucher cập nhật bằng SQL thuần trên bảng `Voucher`: entity `Voucher` gồm 2 bảng (bảng phụ `VoucherPolicy`), câu UPDATE hàng loạt bằng JPQL trên entity như vậy có thể khiến Hibernate tạo bảng tạm, mà tài khoản CSDL của ứng dụng không có quyền.
- VNPAY (Q58): chọn VNPAY thì đơn chờ thanh toán được tạo như đơn COD (giữ hàng và lượt voucher) với `Payment` VNPAY UNPAID, rồi khách được chuyển sang VNPAY với mã giao dịch = mã đơn, hạn thanh toán = giờ đặt + 15 phút; giỏ chưa bị xóa. Đơn chờ không hiện với khách hàng, nhân viên, thống kê (BR-26). Mọi thay đổi khóa dòng `Payment` và kiểm tra còn UNPAID nên IPN và J7 đến cùng lúc không xử lý một đơn hai lần.
- Tác vụ J7 (mỗi phút): đơn chờ đã quá hạn thì gọi API truy vấn (querydr): đã thanh toán → PAID; không có giao dịch (91), chưa hoàn tất hoặc lỗi → xóa đơn chờ; không truy vấn được hoặc kết quả không có chữ ký hợp lệ → giữ nguyên, thử lại lần sau.
- Hủy đơn (FR-17, FR-23, FR-24) dùng chung một xử lý: đơn VNPAY đã thanh toán thì gọi API hoàn tiền trước, chỉ hủy khi hoàn tiền thành công (Q65); sau đó trong một giao dịch: CANCELLED, cộng lại tồn kho từng dòng (kể cả đang giao, Q28), hoàn lượt voucher, COD UNPAID → FAILED hoặc VNPAY PAID → REFUND (xét lại hạng), đóng sự cố giao hàng đang mở; rồi gửi E6. Mọi chuyển trạng thái khóa dòng `Order` và kiểm tra lại trạng thái, chỉ cho phép đúng bảng 6.1.
- Tác vụ hằng ngày: J1 gửi E2 cho đơn bàn giao vận chuyển trong khoảng [hôm nay − 4 ngày, hôm nay − 3 ngày); J2 tự hoàn tất đơn đang giao 7 ngày sau `shippedAt` (hoặc `resolvedAt` nếu đã xử lý sự cố), không khi có sự cố đang mở; J3 hủy đơn COD chưa giao quá 15 ngày. Link trong email do tác vụ gửi dùng `app.baseUrl`.
- `Scheduler` chạy J1 – J7 trên một luồng nền theo giờ Việt Nam: J7 mỗi phút, các tác vụ còn lại lúc 00:00 hằng ngày; mỗi lần chạy đều ghi log. J2, J3, J5, J6 kiểm tra trước khi làm nên chạy bù một lần sau khi khởi động; J1, J4 không chạy bù vì phải chạy đúng một lần mỗi ngày.
- JSP trên Tomcat 11 (EL 6.0) đọc record qua các thành phần của record, không gọi getter hay phương thức khác; giá trị nào trang cần (kể cả giá trị tính ra như `onSale`) phải là thành phần của record.

Giao diện lấy từ template **Shoppers** của Colorlib (giấy phép CC BY 3.0, phải giữ dòng ghi công ở chân trang), dựa trên bản JSP đã Việt hóa của nhóm. Các liên kết tới chức năng chưa làm (đơn hàng, tài khoản...) hiện mở trang 404 sau khi đăng nhập. Ảnh sản phẩm mẫu cũng lấy từ template này.

## Kiểm tra đã chạy

- `mvnw.cmd clean package`: 104 test JUnit đạt (gồm hai khách cùng mua món hàng cuối: chỉ một đơn được tạo, tồn kho không âm), tạo được `target/clothing-shop.war`.
- MySQL Server 26.7.0 (một bản chạy tạm, dữ liệu riêng): `database/schema.sql` tạo đủ 16 bảng, 19 khóa ngoại; Hibernate kiểm tra ánh xạ (`hibernate.hbm2ddl.auto=validate`, chỉ dùng lúc kiểm tra) không báo lỗi.
- Tomcat 11.0.24 (một bản chạy tạm) + MySQL trên: đăng ký sai thì báo lỗi từng ô; đăng ký đúng thì chưa có tài khoản trong CSDL; mở link trong email (lấy từ log) thì tài khoản được tạo, dùng lại link bị từ chối; đăng nhập bằng email viết hoa thành công, sai mật khẩu bị từ chối; đăng xuất; POST thiếu token CSRF bị chặn (403).
- Tomcat 11.0.24 + MySQL 26.7.0 tạm, nạp `seed-dev.sql`: cửa hàng hiện 7 mặt hàng (mặt hàng không bán bị ẩn); lọc danh mục, tên có dấu, màu, khoảng giá sau giảm cho đúng kết quả; giá nhập sai báo lỗi; giá giảm 15% hiện 150.000 → 128.000; trang chi tiết hiện đánh giá (4,5/5); mặt hàng không bán và `id` không tồn tại trả về 404.
- Giỏ hàng trên Tomcat + MySQL tạm, với một khách hàng đăng ký thử: chưa đăng nhập mở `/cart` thì sang trang đăng nhập rồi quay lại; thêm vượt tồn kho, dòng hết hàng, mặt hàng không bán đều bị từ chối với thông báo cụ thể; phiên đăng nhập thứ hai (như trình duyệt khác) thấy cùng giỏ; cập nhật nhiều dòng cùng lúc áp dụng dòng hợp lệ và báo dòng sai; dòng bị chuyển sang không bán hoặc tồn kho giảm được đánh dấu và không tính vào tổng; POST thiếu token CSRF bị chặn.
- Yêu thích trên Tomcat + MySQL tạm: thêm từ trang chi tiết lưu đúng dòng đang chọn, nút đổi thành "Bỏ khỏi danh sách yêu thích"; thêm dòng khác của cùng mặt hàng không tạo bản ghi mới; mặt hàng chuyển sang không bán vẫn hiện, có nhãn "Ngừng bán"; bỏ khỏi danh sách được. Tài khoản nhân viên mở `/cart`, `/wishlist` nhận 403.
- Tài khoản trên Tomcat + MySQL tạm: chưa đăng nhập mở trang tài khoản thì sang trang đăng nhập; lưu thông tin cá nhân, lưu lại từ form cũ bị từ chối; đổi mật khẩu cần mật khẩu cũ đúng; quên mật khẩu với email viết hoa vẫn nhận link (lấy từ log), đặt mật khẩu mới xong thì link hết hiệu lực; xóa địa chỉ mặc định phải chọn hoặc tạo địa chỉ mặc định mới, CSDL luôn còn đúng một địa chỉ mặc định.
- Khu quản lý trên Tomcat + MySQL tạm: tài khoản quản lý được tạo từ cấu hình lúc khởi động; tạo nhân viên, tạo trùng tên/email bị từ chối; nhân viên không vào được `/admin/*` (403); khóa nhân viên thì phiên đang mở bị đăng xuất ở request kế tiếp và không đăng nhập lại được, mở khóa thì đăng nhập được; không khóa được quản lý; khách hàng vào `/staff` nhận 403; nâng hạng, nâng từ trang cũ bị từ chối, bật tự hạ hạng thì hạng được xét lại.
- Đặt hàng trên Tomcat + MySQL tạm: tổng tiền đúng FR-15 (376.000 − voucher 10% làm tròn 38.000 + 20.000 = 358.000); voucher sai hạng, hết lượt, chưa đến ngày, hết hạn, không tồn tại đều báo đúng lý do; thiếu địa chỉ báo lỗi từng ô; tổng tiền đã đổi thì không tạo đơn; đặt thành công thì CSDL có đơn, chi tiết, thanh toán COD UNPAID, tồn kho và lượt voucher bị trừ, địa chỉ mới được lưu, giỏ được dọn.
- VNPAY trên Tomcat + MySQL tạm, với một chương trình Python đóng vai VNPAY (ký và kiểm tra chữ ký HMAC-SHA512 độc lập với mã Java): URL thanh toán có chữ ký đúng, số tiền = tổng tiền × 100, hạn 15 phút; đơn chờ giữ hàng và lượt voucher, giỏ chưa bị xóa; IPN sai chữ ký trả 97, sai số tiền trả 04, thành công trả 00 (thanh toán PAID, giỏ được dọn), gửi lại trả 02; khách hủy thì đơn chờ bị xóa, tồn kho trả lại; J7 gửi querydr có chữ ký đúng, xác nhận đơn đã trả tiền mà lỡ IPN và xóa đơn bỏ dở.
- Vòng đời đơn trên Tomcat + MySQL tạm: bàn giao trước khi xác nhận bị từ chối; xác nhận, bàn giao gửi E1; bấm nút E1 không cần đăng nhập thì đơn giao thành công, COD chuyển PAID, bấm lại không đổi gì; "Chưa thấy hàng đâu" ghi nhận một lần, link bị sửa thì báo không hợp lệ; nhân viên xử lý sự cố, E4 chứa lý do và cách giải quyết đã nhập; khách hủy đơn đang giao thì tồn kho được cộng lại, thanh toán FAILED, E6 đúng lý do; không hủy được đơn đã giao thành công; đơn VNPAY giao không thành công: VNPAY từ chối hoàn tiền thì đơn giữ nguyên, hoàn tiền được thì đơn CANCELLED, thanh toán REFUND (yêu cầu hoàn tiền có chữ ký đúng); sau khi khởi động lại, J2 tự hoàn tất đơn đã giao 8 ngày trước, J3 hủy đơn COD đặt 16 ngày trước.
- **Chưa thử:** gửi email qua SMTP thật; kết nối Aiven; sandbox VNPAY thật (cần mã TMN và khóa bí mật của nhóm).

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

- Các chức năng còn lại của đặc tả: trả hàng, đánh giá, quản lý sản phẩm, voucher tự động, thống kê...
