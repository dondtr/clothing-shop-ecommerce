<%-- Lịch sử đơn hàng (FR-16 bước 1): mới nhất trước; đơn đang chờ thanh toán VNPAY không hiển thị (BR-26). --%>
<c:set var="pageTitle" value="Đơn hàng của tôi"/>
<c:set var="accountNav" value="orders"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Đơn hàng của tôi</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row">
      <div class="col-md-3"><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></div>
      <div class="col-md-9">
        <h2 class="h4 text-black mb-3">Đơn hàng của tôi</h2>
        <c:choose>
          <c:when test="${empty orders}">
            <div class="alert alert-info">Bạn chưa có đơn hàng nào. <a href="${ctx}/shop">Mua sắm ngay</a></div>
          </c:when>
          <c:otherwise>
            <div class="table-responsive">
              <table class="table table-hover">
                <thead class="thead-light"><tr><th>Mã đơn</th><th>Ngày đặt</th><th class="text-right">Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th></tr></thead>
                <tbody>
                  <c:forEach items="${orders}" var="o">
                    <tr>
                      <td><a href="${ctx}/orders/detail?id=${o.id}">#${o.id}</a></td>
                      <td>${f:dateTime(o.orderDate)}</td>
                      <td class="text-right">${f:vnd(o.totalAmount)}</td>
                      <td><small>${o.method}</small> <t:status value="${o.paymentStatus}"/></td>
                      <td><t:status value="${o.status}"/></td>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </div>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
