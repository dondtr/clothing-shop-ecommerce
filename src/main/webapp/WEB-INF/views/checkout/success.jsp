<%-- Đặt hàng thành công (SD03): hiện mã đơn. --%>
<c:set var="pageTitle" value="Đặt hàng thành công"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-8 text-center">
        <span class="icon-check_circle display-3 text-primary"></span>
        <h2 class="display-4 text-black mt-2">Cảm ơn bạn!</h2>
        <p class="lead mb-4">Đơn hàng <strong>#${orderId}</strong> đã được ghi nhận. Cửa hàng sẽ xác nhận và giao hàng sớm.</p>
        <p>
          <a href="${ctx}/orders/detail?id=${orderId}" class="btn btn-sm btn-primary">Xem đơn hàng</a>
          <a href="${ctx}/shop" class="btn btn-sm btn-outline-primary">Tiếp tục mua sắm</a>
        </p>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
