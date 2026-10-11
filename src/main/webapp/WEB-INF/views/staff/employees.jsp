<%-- Nhân viên (quản lý): tạo tài khoản STAFF (FR-40), khóa, mở khóa (FR-41). --%>
<c:set var="pageTitle" value="Nhân viên"/>
<c:set var="staffNav" value="employees"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<h1 class="h3 text-black mb-4">Nhân viên</h1>
<div class="card mb-4">
  <div class="card-body p-0">
    <table class="table table-sm mb-0">
      <thead class="thead-light">
        <tr><th>Tên đăng nhập</th><th>Họ tên</th><th>Email</th><th>Điện thoại</th><th>Vai trò</th><th>Ngày vào làm</th><th>Trạng thái</th><th></th></tr>
      </thead>
      <tbody>
        <c:forEach items="${employees}" var="e">
          <tr>
            <td><c:out value="${e.userName}"/></td>
            <td><c:out value="${e.fullName}"/></td>
            <td><c:out value="${e.email}"/></td>
            <td><c:out value="${e.phone}"/></td>
            <td>${e.role.label}</td>
            <td>${e.hireDate}</td>
            <td><span class="badge ${e.active ? 'badge-success' : 'badge-secondary'}">${e.active ? 'Đang hoạt động' : 'Đã khóa'}</span></td>
            <td class="text-right">
              <c:if test="${e.role == 'STAFF'}">
                <form action="${ctx}/admin/employees/${e.active ? 'lock' : 'unlock'}" method="post" class="m-0"
                      data-confirm="${e.active ? 'Khóa tài khoản này? Nhân viên sẽ bị đăng xuất ngay.' : 'Mở khóa tài khoản này?'}">
                  <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                  <input type="hidden" name="id" value="${e.id}">
                  <button type="submit" class="btn btn-sm ${e.active ? 'btn-outline-danger' : 'btn-outline-success'}">${e.active ? 'Khóa' : 'Mở khóa'}</button>
                </form>
              </c:if>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>

<div class="card">
  <div class="card-body">
    <h2 class="h5 text-black">Tạo tài khoản nhân viên</h2>
    <p class="text-muted small">Chỉ tạo được vai trò nhân viên (STAFF); hệ thống có đúng một tài khoản quản lý (BR-24).
      Tên đăng nhập và email không được trùng với bất kỳ tài khoản nào, kể cả tài khoản khách hàng (BR-27).</p>
    <c:if test="${not empty errors}"><div class="alert alert-danger">Vui lòng kiểm tra lại các ô được đánh dấu.</div></c:if>
    <form action="${ctx}/admin/employees/create" method="post" novalidate>
      <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
      <div class="row">
        <t:field name="userName" label="Tên đăng nhập" value="${param.userName}" required="true" maxlength="50" error="${errors.userName}" cssClass="col-md-4"/>
        <t:field name="email" label="Email" type="email" value="${param.email}" required="true" maxlength="100" error="${errors.email}" cssClass="col-md-4"/>
        <t:field name="fullName" label="Họ và tên" value="${param.fullName}" required="true" maxlength="100" error="${errors.fullName}" cssClass="col-md-4"/>
      </div>
      <div class="row">
        <t:field name="gender" label="Giới tính" required="true" error="${errors.gender}" cssClass="col-md-4">
          <select id="gender" name="gender" class="form-control">
            <option value="FEMALE" ${param.gender == 'FEMALE' ? 'selected' : ''}>Nữ</option>
            <option value="MALE" ${param.gender == 'MALE' ? 'selected' : ''}>Nam</option>
            <option value="OTHER" ${param.gender == 'OTHER' ? 'selected' : ''}>Khác</option>
          </select>
        </t:field>
        <t:field name="dob" label="Ngày sinh" type="date" value="${param.dob}" required="true" error="${errors.dob}" cssClass="col-md-4"/>
        <t:field name="phone" label="Số điện thoại" type="tel" value="${param.phone}" required="true" maxlength="15" error="${errors.phone}" cssClass="col-md-4"/>
      </div>
      <div class="row">
        <t:field name="password" label="Mật khẩu ban đầu" type="password" required="true" maxlength="128" error="${errors.password}"
                 hint="Ít nhất 8 ký tự; nhân viên tự đổi sau khi đăng nhập" cssClass="col-md-4" autocomplete="new-password"/>
        <t:field name="confirmPassword" label="Nhập lại mật khẩu" type="password" required="true" maxlength="128"
                 error="${errors.confirmPassword}" cssClass="col-md-4" autocomplete="new-password"/>
      </div>
      <button type="submit" class="btn btn-primary">Tạo tài khoản</button>
    </form>
  </div>
</div>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
