<%-- Thông tin một khách hàng (FR-42 bước 2); quản lý nâng hạng, bật/tắt tự hạ hạng (FR-28). --%>
<c:set var="pageTitle" value="${customer.fullName}"/>
<c:set var="staffNav" value="customers"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>
<c:set var="c" value="${customer}"/>

<p class="mb-2"><a href="${ctx}/staff/customers">← Danh sách khách hàng</a></p>
<h1 class="h3 text-black mb-4"><c:out value="${c.fullName}"/></h1>
<div class="row">
  <div class="col-lg-5 mb-4">
    <div class="card h-100"><div class="card-body">
      <h2 class="h6 text-uppercase text-muted">Thông tin</h2>
      <dl class="row mb-0">
        <dt class="col-5">Tên đăng nhập</dt><dd class="col-7"><c:out value="${c.userName}"/></dd>
        <dt class="col-5">Email</dt><dd class="col-7"><c:out value="${c.email}"/></dd>
        <dt class="col-5">Điện thoại</dt><dd class="col-7"><c:out value="${c.phone}"/></dd>
        <dt class="col-5">Giới tính</dt><dd class="col-7">${c.gender.label}</dd>
        <dt class="col-5">Ngày sinh</dt><dd class="col-7">${c.dob}</dd>
        <dt class="col-5">Ngày tạo</dt><dd class="col-7">${f:dateTime(c.createdAt)}</dd>
      </dl>
    </div></div>
  </div>
  <div class="col-lg-7 mb-4">
    <div class="card h-100"><div class="card-body">
      <h2 class="h6 text-uppercase text-muted">Hạng thành viên</h2>
      <p class="mb-1">Hạng hiện tại: <strong>${c.level.label}</strong></p>
      <p class="mb-1">Tổng đã thanh toán: <strong>${f:vnd(c.paidTotal)}</strong>
        <small class="text-muted">(từ 2.000.000 ₫: Thường xuyên; từ 10.000.000 ₫: Trung thành)</small></p>
      <p class="mb-3">Tự hạ hạng: <strong>${c.autoDowngrade ? 'Bật' : 'Tắt'}</strong></p>
      <c:if test="${sessionScope.auth.admin}">
        <c:if test="${c.level != 'LOYAL'}">
          <form action="${ctx}/admin/customers/upgrade" method="post" class="form-inline mb-2"
                data-confirm="Nâng hạng sẽ tắt chế độ tự hạ hạng của khách hàng này. Tiếp tục?">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="id" value="${c.id}">
            <input type="hidden" name="version" value="${c.version}">
            <label for="level" class="mr-2">Nâng lên</label>
            <select id="level" name="level" class="form-control form-control-sm mr-2">
              <c:forEach items="${levels}" var="l">
                <c:if test="${l.ordinal() > c.level.ordinal()}"><option value="${l}">${l.label}</option></c:if>
              </c:forEach>
            </select>
            <button type="submit" class="btn btn-sm btn-primary">Nâng hạng</button>
          </form>
        </c:if>
        <form action="${ctx}/admin/customers/auto-downgrade" method="post" class="m-0"
              data-confirm="${c.autoDowngrade ? 'Tắt tự hạ hạng?' : 'Bật tự hạ hạng? Hạng sẽ được xét lại ngay và khách hàng có thể bị hạ hạng.'}">
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <input type="hidden" name="id" value="${c.id}">
          <input type="hidden" name="version" value="${c.version}">
          <input type="hidden" name="value" value="${not c.autoDowngrade}">
          <button type="submit" class="btn btn-sm btn-outline-secondary">${c.autoDowngrade ? 'Tắt tự hạ hạng' : 'Bật tự hạ hạng'}</button>
        </form>
        <p class="text-muted small mt-2 mb-0">Quản lý chỉ nâng hạng, không hạ hạng thủ công (BR-02).</p>
      </c:if>
    </div></div>
  </div>
</div>

<div class="card mb-4"><div class="card-body">
  <h2 class="h6 text-uppercase text-muted mb-3">Lịch sử đơn hàng</h2>
  <c:set var="orderRows" value="${c.orders}"/>
  <%@ include file="/WEB-INF/views/staff/order-table.jspf" %>
</div></div>

<div class="card"><div class="card-body">
  <h2 class="h6 text-uppercase text-muted mb-3">Yêu cầu trả hàng</h2>
  <c:choose>
    <c:when test="${empty c.returns}"><p class="text-muted mb-0">Chưa có yêu cầu trả hàng.</p></c:when>
    <c:otherwise>
      <table class="table table-sm mb-0">
        <thead class="thead-light"><tr><th>Đơn</th><th>Ngày gửi</th><th>Lý do</th><th>Trạng thái</th><th>Ngày xử lý</th></tr></thead>
        <tbody>
          <c:forEach items="${c.returns}" var="r">
            <tr>
              <td><a href="${ctx}/staff/orders/detail?id=${r.orderId}">#${r.orderId}</a></td>
              <td>${f:dateTime(r.requestedAt)}</td>
              <td><c:out value="${r.reason}"/></td>
              <td><t:status value="${r.status}"/></td>
              <td>${f:dateTime(r.handledAt)}</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
</div></div>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
