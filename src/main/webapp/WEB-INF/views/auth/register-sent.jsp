<%-- Sau khi đăng ký: nhắc khách hàng mở email xác thực (FR-01 bước 3, email E8). --%>
<c:set var="pageTitle" value="Kiểm tra email"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-8 col-lg-6 text-center">
        <span class="icon-envelope-o display-3 text-primary"></span>
        <h2 class="h3 mt-3 mb-3 text-black">Kiểm tra hộp thư của bạn</h2>
        <p>Cửa hàng đã gửi link xác thực tới <strong><c:out value="${email}"/></strong>.</p>
        <p>Bấm vào link trong email để hoàn tất đăng ký. Link có hiệu lực trong 24 giờ và chỉ dùng được một lần.</p>
        <p class="text-muted small">Không thấy email? Hãy xem thư mục Spam. Nếu link đã hết hạn, bạn có thể <a href="${ctx}/register">đăng ký lại</a>.</p>
        <a href="${ctx}/" class="btn btn-outline-primary mt-3">Về trang chủ</a>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
