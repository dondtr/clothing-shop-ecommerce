#!/bin/sh
# Tomcat đọc file này trước khi khởi động (catalina.sh). Được chép vào image bởi Dockerfile.

# Cổng HTTP: nền tảng hosting (Render, Railway...) thường đặt biến PORT; mặc định 8080
CATALINA_OPTS="$CATALINA_OPTS -Dport.http=${PORT:-8080}"

# CSDL bắt buộc SSL có chứng chỉ riêng (ví dụ Aiven): đặt nội dung ca.pem vào biến DB_CA_PEM,
# file truststore /tmp/db-truststore.p12 (mật khẩu changeit, chỉ chứa chứng chỉ công khai) được tạo
# để CLOTHINGSHOP_DB_URL dùng sslMode=VERIFY_CA. Xem docs/huong-dan-docker-va-hosting.md.
if [ -n "$DB_CA_PEM" ]; then
  printf '%s\n' "$DB_CA_PEM" > /tmp/db-ca.pem
  rm -f /tmp/db-truststore.p12
  if keytool -importcert -noprompt -alias db-ca -file /tmp/db-ca.pem -keystore /tmp/db-truststore.p12 \
      -storetype PKCS12 -storepass changeit > /dev/null 2>&1; then
    echo "setenv.sh: created /tmp/db-truststore.p12 from DB_CA_PEM"
  else
    echo "setenv.sh: DB_CA_PEM is not a valid PEM certificate, truststore not created" >&2
  fi
fi
