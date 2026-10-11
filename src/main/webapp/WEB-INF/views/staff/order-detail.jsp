<%-- Chi tiết đơn cho nhân viên: xác nhận, bàn giao vận chuyển (FR-18), hủy do giao không thành công (FR-23). --%>
<c:set var="o" value="${order}"/>
<c:set var="pageTitle" value="Đơn #${o.id}"/>
<c:set var="staffNav" value="orders"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<p class="mb-2"><a href="${ctx}/staff/orders">← Danh sách đơn hàng</a></p>
<div class="d-flex justify-content-between align-items-center flex-wrap mb-3">
  <h1 class="h3 text-black mb-0">Đơn #${o.id} <t:status value="${o.status}"/></h1>
  <div class="d-flex flex-wrap" style="gap: 8px;">
    <c:if test="${o.status == 'PENDING'}">
      <form action="${ctx}/staff/orders/confirm" method="post" class="m-0">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="id" value="${o.id}">
        <button type="submit" class="btn btn-primary btn-sm">Xác nhận đơn</button>
      </form>
    </c:if>
    <c:if test="${o.status == 'CONFIRMED'}">
      <form action="${ctx}/staff/orders/ship" method="post" class="m-0" data-confirm="Đã bàn giao đơn #${o.id} cho đơn vị vận chuyển? Khách hàng sẽ nhận email.">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="id" value="${o.id}">
        <button type="submit" class="btn btn-primary btn-sm">Bàn giao vận chuyển</button>
      </form>
    </c:if>
    <c:if test="${o.status == 'SHIPPING'}">
      <form action="${ctx}/staff/orders/failed" method="post" class="m-0"
            data-confirm="Xác nhận đơn vị vận chuyển báo giao không thành công? Đơn sẽ bị hủy, tồn kho và voucher được hoàn${o.paymentStatus == 'PAID' ? ', tiền được hoàn qua VNPAY' : ''}.">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="id" value="${o.id}">
        <button type="submit" class="btn btn-outline-danger btn-sm">Giao không thành công – hủy đơn</button>
      </form>
    </c:if>
  </div>
</div>
<p class="mb-3"><a href="${ctx}/staff/customers/detail?id=${o.customerId}">Khách hàng: <c:out value="${o.customerName}"/></a>
  · <c:out value="${o.customerEmail}"/></p>

<%@ include file="/WEB-INF/views/orders/order-summary.jspf" %>
<c:if test="${not empty o.issue and o.issue.status == 'OPEN'}">
  <a href="${ctx}/staff/delivery-issues" class="btn btn-sm btn-warning">Xử lý báo chưa nhận được hàng</a>
</c:if>
<c:if test="${not empty o.returnRequest and o.returnRequest.status == 'PENDING'}">
  <a href="${ctx}/staff/returns" class="btn btn-sm btn-warning">Xử lý yêu cầu trả hàng</a>
</c:if>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
