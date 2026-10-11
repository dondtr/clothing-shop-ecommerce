<%-- Yêu cầu trả hàng (FR-26): duyệt (hoàn tiền, hủy đơn, hoàn tồn kho và voucher) hoặc từ chối. --%>
<c:set var="pageTitle" value="Yêu cầu trả hàng"/>
<c:set var="staffNav" value="returns"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<h1 class="h3 text-black mb-2">Yêu cầu trả hàng</h1>
<p class="text-muted small mb-4">Duyệt: đơn VNPAY được hoàn tiền qua VNPAY trước (lỗi thì yêu cầu giữ nguyên để thử lại); đơn COD cửa hàng hoàn tiền
  ngoài hệ thống (Q30). Sau đó đơn chuyển sang Đã hủy, tồn kho và lượt voucher được hoàn, hạng thành viên được xét lại.</p>
<div class="card mb-4"><div class="card-body p-0">
  <table class="table table-sm mb-0">
    <thead class="thead-light"><tr><th>Đơn</th><th>Khách hàng</th><th class="text-right">Tổng tiền</th><th>Thanh toán</th><th>Nhận hàng</th><th>Gửi yêu cầu</th><th>Lý do</th><th></th></tr></thead>
    <tbody>
      <c:if test="${empty pending}"><tr><td colspan="8" class="text-center text-muted py-3">Không có yêu cầu nào đang chờ.</td></tr></c:if>
      <c:forEach items="${pending}" var="r">
        <tr>
          <td><a href="${ctx}/staff/orders/detail?id=${r.orderId}">#${r.orderId}</a></td>
          <td><a href="${ctx}/staff/customers/detail?id=${r.customerId}"><c:out value="${r.customerName}"/></a></td>
          <td class="text-right">${f:vnd(r.totalAmount)}</td>
          <td>${r.method}</td>
          <td>${f:dateTime(r.deliveredAt)}</td>
          <td>${f:dateTime(r.requestedAt)}</td>
          <td style="max-width: 280px;"><c:out value="${r.reason}"/></td>
          <td class="text-nowrap">
            <form action="${ctx}/staff/returns/approve" method="post" class="d-inline"
                  data-confirm="Duyệt trả hàng đơn #${r.orderId}? ${r.method == 'VNPAY' ? 'Tiền sẽ được hoàn qua VNPAY.' : 'Cửa hàng cần hoàn tiền cho khách ngoài hệ thống.'}">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="id" value="${r.id}">
              <button type="submit" class="btn btn-sm btn-primary">Duyệt</button>
            </form>
            <form action="${ctx}/staff/returns/reject" method="post" class="d-inline" data-confirm="Từ chối yêu cầu trả hàng đơn #${r.orderId}?">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="id" value="${r.id}">
              <button type="submit" class="btn btn-sm btn-outline-secondary">Từ chối</button>
            </form>
          </td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div></div>

<h2 class="h6 text-uppercase text-muted">Đã xử lý gần đây</h2>
<div class="card"><div class="card-body p-0">
  <table class="table table-sm mb-0">
    <thead class="thead-light"><tr><th>Đơn</th><th>Khách hàng</th><th>Lý do</th><th>Kết quả</th><th>Người xử lý</th><th>Lúc</th></tr></thead>
    <tbody>
      <c:if test="${empty handled}"><tr><td colspan="6" class="text-center text-muted py-3">Chưa có.</td></tr></c:if>
      <c:forEach items="${handled}" var="r">
        <tr>
          <td><a href="${ctx}/staff/orders/detail?id=${r.orderId}">#${r.orderId}</a></td>
          <td><c:out value="${r.customerName}"/></td>
          <td><c:out value="${r.reason}"/></td>
          <td><t:status value="${r.status}"/></td>
          <td><c:out value="${r.handledBy}"/></td>
          <td>${f:dateTime(r.handledAt)}</td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div></div>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
