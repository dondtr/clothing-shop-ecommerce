<%-- Tổng quan khu quản lý: việc đang chờ nhân viên và các đơn mới nhất. --%>
<c:set var="pageTitle" value="Tổng quan"/>
<c:set var="staffNav" value="dashboard"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>
<c:set var="d" value="${dashboard}"/>

<h1 class="h3 text-black mb-4">Tổng quan</h1>
<div class="row">
  <div class="col-md-6 col-xl-3 mb-4">
    <a href="${ctx}/staff/orders?status=PENDING" class="card stat-card text-decoration-none"><div class="card-body">
      <div class="label">Đơn chờ xác nhận</div><div class="value">${d.pendingOrders}</div>
    </div></a>
  </div>
  <div class="col-md-6 col-xl-3 mb-4">
    <a href="${ctx}/staff/orders?status=CONFIRMED" class="card stat-card text-decoration-none"><div class="card-body">
      <div class="label">Đơn chờ bàn giao vận chuyển</div><div class="value">${d.confirmedOrders}</div>
    </div></a>
  </div>
  <div class="col-md-6 col-xl-3 mb-4">
    <a href="${ctx}/staff/delivery-issues" class="card stat-card text-decoration-none"><div class="card-body">
      <div class="label">Sự cố giao hàng chờ xử lý</div><div class="value ${d.openIssues > 0 ? 'text-danger' : ''}">${d.openIssues}</div>
    </div></a>
  </div>
  <div class="col-md-6 col-xl-3 mb-4">
    <a href="${ctx}/staff/returns" class="card stat-card text-decoration-none"><div class="card-body">
      <div class="label">Yêu cầu trả hàng chờ duyệt</div><div class="value">${d.pendingReturns}</div>
    </div></a>
  </div>
</div>

<div class="card mb-4">
  <div class="card-body">
    <h2 class="h6 text-uppercase text-muted mb-3">Đơn theo trạng thái</h2>
    <div class="d-flex flex-wrap" style="gap: 10px;">
      <c:forEach items="${d.orderCounts}" var="e">
        <a href="${ctx}/staff/orders?status=${e.key}" class="btn btn-light border"><t:status value="${e.key}"/> <strong class="ml-1">${e.value}</strong></a>
      </c:forEach>
    </div>
    <p class="text-muted small mt-3 mb-0">${d.customers} khách hàng · ${d.productsOnSale} mặt hàng đang bán.
      Đơn đang chờ thanh toán VNPAY không được tính (BR-26).</p>
  </div>
</div>

<div class="card">
  <div class="card-body">
    <div class="d-flex justify-content-between mb-3">
      <h2 class="h6 text-uppercase text-muted mb-0">Đơn mới nhất</h2>
      <a href="${ctx}/staff/orders" class="small">Xem tất cả</a>
    </div>
    <c:set var="orderRows" value="${d.latestOrders}"/>
    <%@ include file="/WEB-INF/views/staff/order-table.jspf" %>
  </div>
</div>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
