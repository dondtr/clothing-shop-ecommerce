# Hướng dẫn chạy bằng Docker và đưa lên hosting

Tài liệu này hướng dẫn chạy Clothing Shop bằng Docker trên máy cá nhân, cấu hình bằng biến môi trường và đưa lên hosting. Cách chạy trực tiếp trên Tomcat (không dùng Docker) vẫn như trong [README](../README.md).

## 1. Các file Docker trong repo

| File | Vai trò |
|---|---|
| `Dockerfile` | Build WAR bằng Maven (mặc định chạy toàn bộ test), rồi đóng gói vào Tomcat 11 + Java 21. Ứng dụng chạy ở địa chỉ gốc `/` (không có `/clothing-shop`), bằng user thường thay vì root, cổng lấy từ biến `PORT` (mặc định 8080). Thư mục `/data` giữ file thiết lập voucher tự động. Image không chứa mật khẩu hay khóa bí mật nào. |
| `docker/setenv.sh` | Tomcat đọc khi khởi động: đặt cổng theo `PORT`; tạo truststore từ biến `DB_CA_PEM` khi CSDL bắt buộc SSL có chứng chỉ riêng (Aiven). |
| `docker-compose.yml` | Chạy ứng dụng cùng MySQL 8.4. Lần đầu (volume CSDL còn trống), MySQL tự tạo CSDL `clothing_shop` và các bảng từ `database/schema.sql`. MySQL không mở cổng ra ngoài. |
| `.env.example` | Mẫu cấu hình cho Compose. Chép thành `.env` rồi điền; file `.env` đã bị `.gitignore` chặn, không được commit. |
| `.dockerignore` | Không gửi `target/`, `.git/`, file `.env`, `config/*.properties`, chứng chỉ… vào quá trình build. |

## 2. Chạy trên máy cá nhân

**Cần có:** Docker Desktop (Windows, macOS; trên Windows cần bật WSL 2) hoặc Docker Engine kèm Compose (Linux). Không cần cài JDK, Maven, Tomcat hay MySQL. Lần build đầu cần có mạng để tải image và thư viện.

Các lệnh dưới đây chạy trong thư mục gốc của repo (nơi có `docker-compose.yml`).

**Bước 1.** Tạo file `.env` từ mẫu.

```powershell
Copy-Item .env.example .env
```

(Linux, macOS, Git Bash: `cp .env.example .env`.)

**Bước 2.** Mở `.env` và điền tối thiểu:

| Biến | Giá trị |
|---|---|
| `MYSQL_ROOT_PASSWORD`, `DB_PASSWORD` | Hai mật khẩu bất kỳ cho MySQL trong Docker |
| `CLOTHINGSHOP_APP_SECRET` | Chuỗi ngẫu nhiên từ 32 ký tự (khóa ký link trong email) |
| `CLOTHINGSHOP_ADMIN_USERNAME`, `_EMAIL`, `_PASSWORD`, `_FULLNAME`, `_PHONE` | Tài khoản quản lý, được tạo ở lần khởi động đầu; mật khẩu từ 8 ký tự |

Không đặt dấu ngoặc kép quanh giá trị. Tạo chuỗi ngẫu nhiên cho `CLOTHINGSHOP_APP_SECRET` trên Windows PowerShell:

```powershell
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

(Linux, macOS, Git Bash: `openssl rand -base64 32`.)

Email (`CLOTHINGSHOP_MAIL_*`) và VNPAY (`CLOTHINGSHOP_VNPAY_*`) để trống cũng chạy được: email khi đó chỉ được ghi vào log, trang đặt hàng chỉ có COD.

**Bước 3.** Build và chạy. Lần đầu mất vài phút vì phải tải image, tải thư viện và chạy test.

```powershell
docker compose up -d --build
```

**Bước 4.** Mở http://localhost:8080/. Khu quản lý: đăng nhập bằng tài khoản quản lý đã đặt ở bước 2, hệ thống chuyển tới `/staff`.

**Bước 5 (không bắt buộc).** Nạp dữ liệu mẫu (sản phẩm, giảm giá, đánh giá, voucher) để thử. Chỉ làm trên máy cá nhân, một lần:

```powershell
docker compose exec db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 < /database/seed-dev.sql'
```

Lệnh chạy bên trong container nên dùng được cả trên PowerShell lẫn bash.

### Các lệnh thường dùng

| Việc | Lệnh |
|---|---|
| Xem trạng thái | `docker compose ps` |
| Xem log ứng dụng (cả email khi chưa cấu hình SMTP, ví dụ link xác thực đăng ký) | `docker compose logs -f app` |
| Dừng, giữ nguyên dữ liệu | `docker compose stop` |
| Chạy lại | `docker compose start` |
| Build lại sau khi sửa code | `docker compose up -d --build` |
| Build nhanh, bỏ qua test | `docker compose build --build-arg SKIP_TESTS=true` rồi `docker compose up -d` |
| Mở MySQL | `docker compose exec db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" clothing_shop'` |
| Xóa container, giữ dữ liệu | `docker compose down` |
| **Xóa sạch, mất toàn bộ dữ liệu** (CSDL, thiết lập voucher) | `docker compose down -v` |

Lưu ý:

- `database/schema.sql` chỉ chạy khi volume CSDL còn trống (lần đầu). Nếu sau này `schema.sql` thay đổi, phải tự sửa CSDL bằng lệnh SQL, hoặc chạy `docker compose down -v` rồi `up` lại (mất dữ liệu).
- Tài khoản và mật khẩu MySQL (`DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`) cũng chỉ được tạo lần đầu. Đổi trong `.env` sau đó thì phải đổi cả trong MySQL, hoặc xóa sạch và tạo lại.
- Mặc định cổng 8080 chỉ mở cho chính máy này (`APP_BIND=127.0.0.1`). Muốn máy khác trong mạng truy cập thì đặt `APP_BIND=0.0.0.0`. Cổng 8080 đã bị chiếm thì đổi `APP_PORT` (và đổi theo `CLOTHINGSHOP_APP_BASEURL`).
- Giỏ hàng nằm trong bộ nhớ của ứng dụng (đặc tả mục 3.4), nên mất khi container ứng dụng khởi động lại.

## 3. Biến môi trường

Ứng dụng đọc mỗi khóa cấu hình từ biến môi trường `CLOTHINGSHOP_` + tên khóa viết hoa, dấu chấm thành gạch dưới, ví dụ `db.url` → `CLOTHINGSHOP_DB_URL`, `admin.userName` → `CLOTHINGSHOP_ADMIN_USERNAME`. Ý nghĩa từng khóa xem README, mục Cơ sở dữ liệu (2. Cấu hình kết nối, 4. Tài khoản quản lý, 5. VNPAY, 6. Cấu hình khác).

| Biến | Ghi chú |
|---|---|
| `CLOTHINGSHOP_DB_URL`, `CLOTHINGSHOP_DB_USERNAME`, `CLOTHINGSHOP_DB_PASSWORD` | Bắt buộc. Khi chạy bằng `docker-compose.yml` thì được đặt sẵn để nối tới MySQL trong Compose |
| `CLOTHINGSHOP_DB_POOL_MAXSIZE` | Số kết nối tối đa (mặc định 10; Aiven gói nhỏ nên đặt 5) |
| `CLOTHINGSHOP_APP_SECRET` | Nên có; thiếu thì mỗi lần khởi động tự sinh khóa tạm, link đã gửi qua email hết hiệu lực |
| `CLOTHINGSHOP_APP_BASEURL` | Địa chỉ gốc để tạo link trong email và Return URL của VNPAY, ví dụ `https://shop.example.com` (image chạy ở `/`, không thêm `/clothing-shop`) |
| `CLOTHINGSHOP_ADMIN_*` | Tài khoản quản lý, chỉ dùng khi chưa có tài khoản quản lý |
| `CLOTHINGSHOP_MAIL_SMTP_HOST`, `_PORT`, `_USERNAME`, `_PASSWORD`, `_STARTTLS`, `CLOTHINGSHOP_MAIL_FROM` | Gửi email thật; trống host thì email ghi vào log |
| `CLOTHINGSHOP_VNPAY_TMNCODE`, `CLOTHINGSHOP_VNPAY_HASHSECRET`, `CLOTHINGSHOP_VNPAY_PROCESSRETURNURL`… | VNPAY, xem README mục 5 |
| `CLOTHINGSHOP_VOUCHER_SETTINGSFILE` | Image đặt sẵn `/data/voucher-settings.json` |
| `CLOTHINGSHOP_SCHEDULER_ENABLED` | Mặc định `true`; chỉ một bản ứng dụng được chạy tác vụ J1 – J7 |
| `PORT` | Cổng HTTP trong container (mặc định 8080); nền tảng hosting thường tự đặt |
| `DB_CA_PEM` | Nội dung file `ca.pem` của CSDL (mục 4.2) |

Riêng `docker-compose.yml` dùng thêm `MYSQL_ROOT_PASSWORD`, `DB_USERNAME`, `DB_PASSWORD`, `APP_BIND`, `APP_PORT`.

## 4. Đưa lên hosting

Dù chọn cách nào, cần bảo đảm:

1. **Chỉ chạy một bản ứng dụng** (không bật nhiều instance hay tự co giãn): tác vụ tự động J1 – J7 chạy bên trong ứng dụng, giỏ hàng nằm trong bộ nhớ, thiết lập voucher nằm trong file.
2. **Ứng dụng chạy liên tục:** J1, J4 chạy lúc 00:00, J7 chạy mỗi phút. Gói miễn phí kiểu "ngủ khi không có truy cập" làm các tác vụ này không chạy đúng giờ và mất giỏ hàng mỗi lần thức dậy; chỉ hợp để demo.
3. **Lưu trữ bền cho `/data`:** nếu không có, mỗi lần deploy lại thì thiết lập voucher tự động trở về mặc định (voucher đã phát hành vẫn nằm trong CSDL).
4. **Tên miền có HTTPS**, `CLOTHINGSHOP_APP_BASEURL=https://<tên miền>`; VNPAY cần gọi được IPN tới `https://<tên miền>/payment/vnpay-ipn`.
5. **CSDL MySQL 8.4 trở lên** đã có bảng từ `database/schema.sql` trước lần chạy đầu (Compose tự làm; CSDL bên ngoài thì tự chạy).

### 4.1. Cách A: máy chủ ảo (VPS) chạy Docker Compose

Hợp nhất với đồ án vì giống hệt cách chạy trên máy cá nhân, dữ liệu nằm trong volume của máy chủ.

1. Thuê VPS Ubuntu (khuyến nghị từ 2 GB RAM) và trỏ bản ghi A của tên miền về IP của VPS.
2. Trên VPS, cài Docker Engine theo hướng dẫn tại https://docs.docker.com/engine/install/ubuntu/.
3. Lấy mã nguồn và tạo cấu hình:
   ```bash
   git clone https://github.com/dondtr/clothing-shop-ecommerce.git
   cd clothing-shop-ecommerce
   cp .env.example .env
   nano .env
   ```
   Điền như mục 2, thêm `CLOTHINGSHOP_APP_BASEURL=https://<tên miền>`, email và VNPAY. Giữ `APP_BIND=127.0.0.1` để chỉ reverse proxy truy cập được ứng dụng.
4. Chạy: `docker compose up -d --build`.
5. Cài Caddy làm reverse proxy có HTTPS tự động (Let's Encrypt), theo https://caddyserver.com/docs/install. File `/etc/caddy/Caddyfile`:
   ```
   shop.example.com {
       reverse_proxy 127.0.0.1:8080
   }
   ```
   Rồi `sudo systemctl reload caddy`. Caddy gửi kèm `X-Forwarded-For`, `X-Forwarded-Proto`; image đã bật RemoteIpValve để ứng dụng nhận đúng IP và scheme thật.
6. Chỉ mở cổng 22, 80, 443:
   ```bash
   sudo ufw allow OpenSSH
   sudo ufw allow 80,443/tcp
   sudo ufw enable
   ```
7. Cập nhật phiên bản mới: `git pull && docker compose up -d --build`.
8. Sao lưu và khôi phục CSDL (chạy trên VPS):
   ```bash
   docker compose exec -T db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction clothing_shop' > backup.sql
   docker compose exec -T db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" clothing_shop' < backup.sql
   ```

### 4.2. Cách B: nền tảng chạy container và MySQL trên Aiven

Dùng cho các nền tảng build thẳng từ `Dockerfile` trong repo GitHub (ví dụ Render, Railway, Fly.io, Koyeb). Giao diện, giới hạn và giá mỗi nơi khác nhau và hay thay đổi, nên xem tài liệu của nền tảng; nhớ điều kiện 1 – 3 ở trên.

1. Tạo MySQL service trên Aiven (xem README, mục 3. Chuyển sang Aiven), tải `ca.pem` ở trang Overview và tạo bảng:
   ```bash
   mysql --host=<host> --port=<port> --user=avnadmin --password --ssl-mode=REQUIRED < database/schema.sql
   ```
   Không nạp `seed-dev.sql` lên Aiven.
2. Trên nền tảng hosting, tạo dịch vụ từ repo GitHub, kiểu Docker (dùng `Dockerfile` ở thư mục gốc). Muốn build nhanh hơn thì thêm build argument `SKIP_TESTS=true`.
3. Đặt biến môi trường:

   | Biến | Giá trị |
   |---|---|
   | `CLOTHINGSHOP_DB_URL` | `jdbc:mysql://<host>:<port>/clothing_shop?sslMode=VERIFY_CA&trustCertificateKeyStoreUrl=file:/tmp/db-truststore.p12&trustCertificateKeyStoreType=PKCS12&trustCertificateKeyStorePassword=changeit` |
   | `DB_CA_PEM` | Toàn bộ nội dung `ca.pem`, từ dòng `-----BEGIN CERTIFICATE-----` đến `-----END CERTIFICATE-----` |
   | `CLOTHINGSHOP_DB_USERNAME`, `CLOTHINGSHOP_DB_PASSWORD` | `avnadmin` và mật khẩu trên Aiven |
   | `CLOTHINGSHOP_DB_POOL_MAXSIZE` | `5` |
   | `CLOTHINGSHOP_APP_SECRET`, `CLOTHINGSHOP_APP_BASEURL`, `CLOTHINGSHOP_ADMIN_*`, `CLOTHINGSHOP_MAIL_*`, `CLOTHINGSHOP_VNPAY_*` | Như mục 3; `APP_BASEURL` là địa chỉ HTTPS nền tảng cấp hoặc tên miền riêng |

   Khi khởi động, `docker/setenv.sh` tạo `/tmp/db-truststore.p12` từ `DB_CA_PEM` (log có dòng `setenv.sh: created /tmp/db-truststore.p12 from DB_CA_PEM`), để kết nối vừa mã hóa vừa kiểm tra chứng chỉ của Aiven. Truststore chỉ chứa chứng chỉ công khai nên mật khẩu `changeit` không phải bí mật. Chỉ để thử nhanh thì có thể dùng `sslMode=REQUIRED` và bỏ `DB_CA_PEM` (vẫn mã hóa nhưng không kiểm tra chứng chỉ).
4. Cổng: image nghe ở cổng trong biến `PORT` mà nền tảng đặt; nếu nền tảng hỏi cổng của ứng dụng thì nhập 8080. Đường dẫn health check: `/`.
5. Gắn ổ lưu trữ bền vào `/data` nếu nền tảng hỗ trợ; đặt số instance là 1.

### 4.3. VNPAY và email khi đã có tên miền

- **VNPAY:** đăng ký IPN URL `https://<tên miền>/payment/vnpay-ipn` trong trang merchant; Return URL được tạo theo `CLOTHINGSHOP_APP_BASEURL`. Trên hosting để `CLOTHINGSHOP_VNPAY_PROCESSRETURNURL=false` vì VNPAY gọi được IPN.
- **Email:** ví dụ Gmail: `CLOTHINGSHOP_MAIL_SMTP_HOST=smtp.gmail.com`, `CLOTHINGSHOP_MAIL_SMTP_PORT=587`, `CLOTHINGSHOP_MAIL_SMTP_STARTTLS=true`, tên đăng nhập là địa chỉ Gmail, mật khẩu là mật khẩu ứng dụng (App Password, cần bật Xác minh 2 bước), `CLOTHINGSHOP_MAIL_FROM` là chính địa chỉ Gmail đó. Ứng dụng chỉ hỗ trợ cổng 587 có STARTTLS.

## 5. Lỗi thường gặp

| Hiện tượng | Nguyên nhân, cách sửa |
|---|---|
| `docker compose up` báo `required variable ... is missing a value` | Chưa tạo hoặc chưa điền `.env` (bước 1, 2) |
| Trang báo lỗi 500, log có `Missing setting 'db.url'` | Chạy image mà không đặt `CLOTHINGSHOP_DB_URL` (khi không dùng `docker-compose.yml`) |
| Log có `Access denied for user` | Sai mật khẩu CSDL; với Compose, có thể đã đổi `DB_PASSWORD` sau lần chạy đầu (xem lưu ý mục 2) |
| Log có `Table ... doesn't exist` | CSDL chưa có bảng: CSDL bên ngoài chưa chạy `schema.sql`, hoặc volume cũ được tạo trước khi có bảng |
| Log có `Communications link failure` khi dùng Aiven | Sai host/port, sai `DB_CA_PEM` (chứng chỉ không khớp) hoặc thiếu dòng `setenv.sh: created ...` trong log |
| Không đăng nhập được khu quản lý | Chưa đặt `CLOTHINGSHOP_ADMIN_*` ở lần khởi động đầu; đặt rồi chạy lại `docker compose up -d`, tài khoản được tạo khi khởi động nếu chưa có quản lý |
| Báo cổng 8080 đã được dùng | Đổi `APP_PORT` trong `.env` |
| Link trong email trỏ sai địa chỉ | Sửa `CLOTHINGSHOP_APP_BASEURL` |

## 6. Phạm vi đã kiểm tra

Đã chạy thật trên Docker Desktop (Windows, Docker 29.8, Compose 5.5):

- Build image với toàn bộ test; `docker compose up` tạo đủ 16 bảng, collation `utf8mb4_0900_as_ci`, giờ GMT+7; tài khoản quản lý được tạo đúng một lần; Tomcat chạy bằng user `shop`.
- Nạp dữ liệu mẫu bằng lệnh ở bước 5, tiếng Việt hiển thị đúng; các trang cửa hàng và khu quản lý mở được; khởi động lại vẫn giữ dữ liệu và file thiết lập voucher.
- `PORT=9090` đổi được cổng; `DB_CA_PEM` tạo được truststore; kết nối `sslMode=VERIFY_CA` thành công (TLS 1.3) với đúng CA và bị từ chối với CA sai; thiếu biến trong `.env` thì Compose báo rõ biến nào.

**Chưa thử:** VPS thật, Caddy và HTTPS, các nền tảng Render/Railway/Fly.io/Koyeb, Aiven thật, VNPAY sandbox thật, gửi email SMTP thật.
