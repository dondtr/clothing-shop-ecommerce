<%-- Tra cứu khách hàng (FR-42 bước 1): tìm theo tên, email, số điện thoại. --%>
<c:set var="pageTitle" value="Khách hàng"/>
<c:set var="staffNav" value="customers"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<div class="d-flex justify-content-between align-items-center flex-wrap mb-4">
  <h1 class="h3 text-black mb-0">Khách hàng</h1>
  <a href="${ctx}/staff/delivery-issues" class="btn btn-sm btn-outline-danger">Thông báo sự cố giao hàng</a>
</div>
<form action="${ctx}/staff/customers" method="get" class="form-inline mb-3">
  <input type="text" name="q" value="${fn:escapeXml(param.q)}" class="form-control form-control-sm mr-2" style="min-width: 280px;"
         placeholder="Tên, email hoặc số điện thoại" maxlength="100">
  <button type="submit" class="btn btn-sm btn-primary">Tìm</button>
</form>
<div class="card">
  <div class="card-body p-0">
    <table class="table table-sm table-hover mb-0">
      <thead class="thead-light"><tr><th>Họ tên</th><th>Tên đăng nhập</th><th>Email</th><th>Điện thoại</th><th>Hạng</th><th>Ngày tạo</th></tr></thead>
      <tbody>
        <c:if test="${empty customers}"><tr><td colspan="6" class="text-center text-muted py-3">Không tìm thấy khách hàng.</td></tr></c:if>
        <c:forEach items="${customers}" var="c">
          <tr>
            <td><a href="${ctx}/staff/customers/detail?id=${c.id}"><c:out value="${c.fullName}"/></a></td>
            <td><c:out value="${c.userName}"/></td>
            <td><c:out value="${c.email}"/></td>
            <td><c:out value="${c.phone}"/></td>
            <td>${c.level.label}</td>
            <td>${f:dateTime(c.createdAt)}</td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>
<p class="text-muted small mt-2">Hiển thị tối đa 100 khách hàng; nhập từ khóa để thu hẹp. Tài khoản khách hàng không bị khóa (Q10).</p>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
