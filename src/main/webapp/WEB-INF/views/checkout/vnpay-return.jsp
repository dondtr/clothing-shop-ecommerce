<%-- Trang kết quả VNPAY (FR-13 bước 5): chỉ hiển thị theo trạng thái thanh toán; IPN chưa tới thì tự kiểm tra lại. --%>
<c:set var="pageTitle" value="Kết quả thanh toán"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<c:if test="${status == 'WAITING'}">
  <%-- Kiểm tra lại sau vài giây bằng chính link VNPAY trả về (có chữ ký) --%>
  <script>setTimeout(function () { window.location.reload(); }, 4000);</script>
</c:if>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-8 text-center">
        <c:choose>
          <c:when test="${status == 'PAID'}">
            <span class="icon-check_circle display-3 text-primary"></span>
            <h2 class="h3 text-black mt-2">Giao dịch thành công</h2>
            <p class="lead">Đơn hàng <strong>#<c:out value="${orderId}"/></strong> đã được thanh toán qua VNPAY.</p>
            <p><a href="${ctx}/orders/detail?id=${fn:escapeXml(orderId)}" class="btn btn-sm btn-primary">Xem đơn hàng</a>
              <a href="${ctx}/shop" class="btn btn-sm btn-outline-primary">Tiếp tục mua sắm</a></p>
          </c:when>
          <c:when test="${status == 'WAITING'}">
            <div class="spinner-border text-primary" role="status"></div>
            <h2 class="h3 text-black mt-3">Đang xử lý thanh toán</h2>
            <p>Cửa hàng đang chờ kết quả từ VNPAY cho đơn <strong>#<c:out value="${orderId}"/></strong>. Trang sẽ tự kiểm tra lại.
              Bạn có thể đóng trang này: khi thanh toán thành công, đơn sẽ hiện trong mục Đơn hàng của tôi.</p>
          </c:when>
          <c:when test="${status == 'FAILED'}">
            <span class="icon-close display-3 text-danger"></span>
            <h2 class="h3 text-black mt-2">Thanh toán không thành công</h2>
            <p>Giao dịch bị lỗi, bị hủy hoặc đã quá thời gian thanh toán. Không có đơn hàng nào được tạo và giỏ hàng vẫn giữ nguyên.</p>
            <p><a href="${ctx}/checkout" class="btn btn-sm btn-primary">Quay lại đặt hàng</a></p>
          </c:when>
          <c:otherwise>
            <h2 class="h3 text-black">Kết quả không hợp lệ</h2>
            <p>Không xác minh được dữ liệu VNPAY gửi về. Vui lòng kiểm tra mục Đơn hàng của tôi.</p>
            <p><a href="${ctx}/orders" class="btn btn-sm btn-primary">Đơn hàng của tôi</a></p>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
