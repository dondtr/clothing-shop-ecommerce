<%-- Đăng ký tài khoản khách hàng (FR-01). Lỗi theo từng ô: requestScope.errors (tên ô -> thông báo). --%>
<c:set var="pageTitle" value="Đăng ký"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <div class="row">
      <div class="col-md-12 mb-0"><a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Đăng ký</strong></div>
    </div>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-9 col-lg-7">
        <h2 class="h3 mb-3 text-black">Tạo tài khoản khách hàng</h2>
        <form action="${ctx}/register" method="post" class="p-3 p-lg-5 border" novalidate>
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
          </c:if>
          <c:if test="${not empty errors}">
            <div class="alert alert-danger">Vui lòng kiểm tra lại các ô được đánh dấu.</div>
          </c:if>

          <div class="form-group row">
            <div class="col-md-6">
              <label for="userName" class="text-black form-required">Tên đăng nhập</label>
              <input type="text" class="form-control ${not empty errors.userName ? 'is-invalid' : ''}" id="userName" name="userName"
                     required maxlength="50" pattern="[^@\s]+" value="${fn:escapeXml(param.userName)}" autocomplete="username">
              <c:choose>
                <c:when test="${not empty errors.userName}"><div class="invalid-feedback"><c:out value="${errors.userName}"/></div></c:when>
                <c:otherwise><small class="text-muted">Tối đa 50 ký tự, không chứa @ hay khoảng trắng</small></c:otherwise>
              </c:choose>
            </div>
            <div class="col-md-6">
              <label for="fullName" class="text-black form-required">Họ và tên</label>
              <input type="text" class="form-control ${not empty errors.fullName ? 'is-invalid' : ''}" id="fullName" name="fullName"
                     required maxlength="100" value="${fn:escapeXml(param.fullName)}" autocomplete="name">
              <c:if test="${not empty errors.fullName}"><div class="invalid-feedback"><c:out value="${errors.fullName}"/></div></c:if>
            </div>
          </div>

          <div class="form-group row">
            <div class="col-md-6">
              <label for="email" class="text-black form-required">Email</label>
              <input type="email" class="form-control ${not empty errors.email ? 'is-invalid' : ''}" id="email" name="email"
                     required maxlength="100" value="${fn:escapeXml(param.email)}" autocomplete="email">
              <c:choose>
                <c:when test="${not empty errors.email}"><div class="invalid-feedback"><c:out value="${errors.email}"/></div></c:when>
                <c:otherwise><small class="text-muted">Dùng để xác thực tài khoản; không đổi được sau khi đăng ký</small></c:otherwise>
              </c:choose>
            </div>
            <div class="col-md-6">
              <label for="phone" class="text-black form-required">Số điện thoại</label>
              <input type="tel" class="form-control ${not empty errors.phone ? 'is-invalid' : ''}" id="phone" name="phone"
                     required maxlength="15" pattern="\+?[0-9]{8,14}" value="${fn:escapeXml(param.phone)}" placeholder="09xxxxxxxx" autocomplete="tel">
              <c:if test="${not empty errors.phone}"><div class="invalid-feedback"><c:out value="${errors.phone}"/></div></c:if>
            </div>
          </div>

          <div class="form-group row">
            <div class="col-md-6">
              <label for="gender" class="text-black form-required">Giới tính</label>
              <select id="gender" name="gender" class="form-control ${not empty errors.gender ? 'is-invalid' : ''}" required>
                <option value="">-- Chọn --</option>
                <option value="FEMALE" ${param.gender == 'FEMALE' ? 'selected' : ''}>Nữ</option>
                <option value="MALE" ${param.gender == 'MALE' ? 'selected' : ''}>Nam</option>
                <option value="OTHER" ${param.gender == 'OTHER' ? 'selected' : ''}>Khác</option>
              </select>
              <c:if test="${not empty errors.gender}"><div class="invalid-feedback"><c:out value="${errors.gender}"/></div></c:if>
            </div>
            <div class="col-md-6">
              <label for="dob" class="text-black form-required">Ngày sinh</label>
              <input type="date" class="form-control ${not empty errors.dob ? 'is-invalid' : ''}" id="dob" name="dob"
                     required value="${fn:escapeXml(param.dob)}" autocomplete="bday">
              <c:if test="${not empty errors.dob}"><div class="invalid-feedback"><c:out value="${errors.dob}"/></div></c:if>
            </div>
          </div>

          <div class="form-group row">
            <div class="col-md-6">
              <label for="password" class="text-black form-required">Mật khẩu</label>
              <input type="password" class="form-control ${not empty errors.password ? 'is-invalid' : ''}" id="password" name="password"
                     required minlength="8" maxlength="128" autocomplete="new-password">
              <c:choose>
                <c:when test="${not empty errors.password}"><div class="invalid-feedback"><c:out value="${errors.password}"/></div></c:when>
                <c:otherwise><small class="text-muted">Ít nhất 8 ký tự</small></c:otherwise>
              </c:choose>
            </div>
            <div class="col-md-6">
              <label for="confirmPassword" class="text-black form-required">Nhập lại mật khẩu</label>
              <input type="password" class="form-control ${not empty errors.confirmPassword ? 'is-invalid' : ''}" id="confirmPassword"
                     name="confirmPassword" required minlength="8" maxlength="128" autocomplete="new-password">
              <c:if test="${not empty errors.confirmPassword}"><div class="invalid-feedback"><c:out value="${errors.confirmPassword}"/></div></c:if>
            </div>
          </div>

          <p class="small text-muted">Sau khi bấm Đăng ký, cửa hàng gửi link xác thực tới email của bạn. Tài khoản chỉ được tạo khi bạn bấm link trong 24 giờ.</p>
          <div class="form-group mb-0">
            <button type="submit" class="btn btn-primary btn-lg btn-block">Đăng ký</button>
          </div>
          <p class="mt-4 mb-0 text-center">Đã có tài khoản? <a href="${ctx}/login">Đăng nhập</a></p>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
