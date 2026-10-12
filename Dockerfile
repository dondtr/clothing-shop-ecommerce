# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# Image chạy Clothing Shop trên Tomcat 11 + Java 21, ở context gốc (http://<máy chủ>:8080/).
#   docker build -t clothing-shop .
# Cấu hình bằng biến môi trường CLOTHINGSHOP_<KHÓA> (db.url -> CLOTHINGSHOP_DB_URL, ...),
# xem docs/huong-dan-docker-va-hosting.md. Image không chứa mật khẩu hay khóa bí mật nào.
# ---------------------------------------------------------------------------

# --- Giai đoạn 1: build WAR bằng Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Tải thư viện trước để Docker giữ cache lớp này khi chỉ sửa mã nguồn
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Mặc định chạy toàn bộ test (H2 trong bộ nhớ, không cần MySQL). Build nhanh hơn: --build-arg SKIP_TESTS=true
ARG SKIP_TESTS=false
RUN mvn -B -q package -DskipTests=${SKIP_TESTS}

# --- Giai đoạn 2: chạy ---
FROM tomcat:11.0-jre21-temurin

# - RemoteIpValve: chạy sau reverse proxy (Caddy, Nginx, nền tảng hosting) thì lấy IP và scheme thật
#   từ X-Forwarded-For / X-Forwarded-Proto.
# - Cổng HTTP lấy từ biến PORT nếu nền tảng hosting đặt, mặc định 8080 (xem docker/setenv.sh).
# - Bỏ ứng dụng mặc định của Tomcat; chạy bằng user thường thay vì root.
RUN sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.RemoteIpValve" remoteIpHeader="X-Forwarded-For" protocolHeader="X-Forwarded-Proto" />\n      </Host>#' conf/server.xml \
    && sed -i 's#<Connector port="8080"#<Connector port="${port.http}"#' conf/server.xml \
    && rm -rf webapps/* \
    && groupadd --system shop \
    && useradd --system --gid shop --home-dir /data --no-create-home shop \
    && mkdir -p /data conf/Catalina/localhost \
    && chown -R shop:shop /data logs temp work webapps conf/Catalina

COPY --chown=shop:shop docker/setenv.sh bin/setenv.sh
# Triển khai dạng thư mục: Tomcat không phải giải nén WAR mỗi lần khởi động
COPY --from=build --chown=shop:shop /src/target/clothing-shop webapps/ROOT

ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Ho_Chi_Minh -Dfile.encoding=UTF-8" \
    CLOTHINGSHOP_VOUCHER_SETTINGSFILE=/data/voucher-settings.json

USER shop
# File thiết lập voucher tự động (mục 3.5 của đặc tả) nằm ở /data, cần giữ qua các lần chạy lại
VOLUME /data
EXPOSE 8080

# Kiểm tra trang chủ; dùng lại một cookie để không tạo phiên (session) mới mỗi lần kiểm tra
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS -o /dev/null -c /tmp/healthcheck.cookie -b /tmp/healthcheck.cookie "http://127.0.0.1:${PORT:-8080}/" || exit 1

CMD ["catalina.sh", "run"]
