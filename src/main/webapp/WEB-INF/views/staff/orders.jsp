<%-- Đơn hàng cho nhân viên, lọc theo trạng thái; đơn chờ thanh toán VNPAY không hiển thị (BR-26). --%>
<c:set var="pageTitle" value="Đơn hàng"/>
<c:set var="staffNav" value="orders"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<h1 class="h3 text-black mb-3">Đơn hàng</h1>
<div class="d-flex flex-wrap mb-3" style="gap: 8px;">
  <a href="${ctx}/staff/orders" class="btn btn-sm ${empty status ? 'btn-primary' : 'btn-light border'}">Tất cả</a>
  <c:forEach items="${statuses}" var="s">
    <a href="${ctx}/staff/orders?status=${s}" class="btn btn-sm ${status == s ? 'btn-primary' : 'btn-light border'}">${s.label}</a>
  </c:forEach>
</div>
<div class="card"><div class="card-body p-0">
  <%@ include file="/WEB-INF/views/staff/order-table.jspf" %>
</div></div>
<p class="text-muted small mt-2">Hiển thị tối đa 300 đơn mới nhất.</p>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
