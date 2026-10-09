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
    │   └── util/         JpaUtil: tạo EntityManagerFactory
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

## Cấu hình kết nối CSDL

Thông tin kết nối **không nằm trong mã nguồn**. Khi tạo `EntityManagerFactory` lần đầu, `com.clothingshop.util.JpaUtil` đọc các biến môi trường dưới đây. Nếu thiếu biến môi trường thì đọc system property tương ứng của JVM.

| Biến môi trường | System property | Bắt buộc | Ví dụ |
|---|---|---|---|
| `CLOTHINGSHOP_DB_URL` | `clothingshop.db.url` | Có | `jdbc:mysql://localhost:3306/clothing_shop?connectionTimeZone=Asia/Ho_Chi_Minh` |
| `CLOTHINGSHOP_DB_USERNAME` | `clothingshop.db.username` | Có | `clothing_app` |
| `CLOTHINGSHOP_DB_PASSWORD` | `clothingshop.db.password` | Không (mặc định rỗng) | |

Ví dụ cho Tomcat trên Windows: tạo file `%CATALINA_BASE%\bin\setenv.bat` (nằm ngoài repo, **không commit**):

```bat
set "CLOTHINGSHOP_DB_URL=jdbc:mysql://localhost:3306/clothing_shop?connectionTimeZone=Asia/Ho_Chi_Minh"
set "CLOTHINGSHOP_DB_USERNAME=clothing_app"
set "CLOTHINGSHOP_DB_PASSWORD=<mật khẩu của bạn>"
set "CATALINA_OPTS=%CATALINA_OPTS% -Duser.timezone=Asia/Ho_Chi_Minh"
```

Nếu Tomcat chạy dưới dạng Windows service thì biến môi trường đặt trong `setenv.bat` không có tác dụng. Khi đó dùng `Tomcat11w.exe` (tab Java, mục Java Options) để thêm `-Dclothingshop.db.url=...`, `-Dclothingshop.db.username=...`, `-Dclothingshop.db.password=...`.

Lưu ý:
- Ứng dụng **không tạo, sửa hay xóa schema** (`jakarta.persistence.schema-generation.database.action=none`). CSDL và các bảng phải được tạo riêng theo ERD trước khi chạy; bước này chưa làm trong giai đoạn khởi tạo.
- Tên bảng `Order` trùng từ khóa cấm của MySQL nên đã được đặt trong dấu `` ` ``. Trên Windows, MySQL mặc định lưu tên bảng bằng chữ thường (`lower_case_table_names=1`); trên Linux thì phân biệt hoa thường. Cả nhóm nên thống nhất một thiết lập.

## Deploy lên Tomcat 11

1. Đặt các biến cấu hình CSDL như trên.
2. Chép `target/clothing-shop.war` vào thư mục `webapps` của Tomcat, hoặc deploy qua Tomcat Manager.
3. Ứng dụng chạy ở đường dẫn `/clothing-shop`.

Giai đoạn này chưa có trang nào để mở. Việc deploy và kết nối CSDL **chưa được chạy thử**.

## Mô hình entity

Entity bám theo ERD Clothing E-Commerce (sửa lần 3): 14 entity, 2 lớp nhúng (`OrderDetailId`, `VoucherPolicy`), 12 enum. Các quyết định ánh xạ:

- **Biến thể sản phẩm:** theo ERD, mỗi dòng `Product` là một biến thể (màu + kích thước) có tồn kho riêng. Các dòng cùng `name` là một mặt hàng; (`name`, `color`, `size`) là duy nhất.
- **User, Customer, Employee:** kế thừa `JOINED`. Mỗi tài khoản là đúng một khách hàng hoặc một nhân viên, không dùng chung cho cả hai nghiệp vụ.
- **VoucherPolicy:** lớp nhúng của `Voucher`, các cột lưu ở bảng phụ `VoucherPolicy` (cùng khóa `voucherCode`).
- **WishlistItem:** bảng nối của quan hệ `@ManyToMany` giữa `Wishlist` và `Product`, không có lớp entity riêng.
- **Quan hệ:**
  - Mọi `@ManyToOne` / `@OneToOne` đều `LAZY`.
  - Không đặt chiều ngược cho các quan hệ one-to-one (Payment, DeliveryIssue, ReturnRequest → Order).
  - Cascade chỉ dùng cho quan hệ thành phần: `Order.details`, `Customer.addresses`.
- **Kiểm tra dữ liệu:** các ràng buộc CHECK của ERD (số lượng > 0, rating 1–5, tồn kho ≥ 0) sẽ do tầng service kiểm tra.

## Chưa làm (theo phạm vi giai đoạn khởi tạo)

- Controller, service, DAO, các trang JSP; đăng nhập, giỏ hàng, đặt hàng, thanh toán VNPAY, quản lý tồn kho.
- Tạo CSDL và bảng; dữ liệu mẫu.
- Đóng `EntityManagerFactory` khi ứng dụng dừng (gọi `JpaUtil.close()` từ một `ServletContextListener` khi làm tầng DAO).
- JSTL: thêm khi làm trang JSP đầu tiên.
- Kiểm thử tự động.
