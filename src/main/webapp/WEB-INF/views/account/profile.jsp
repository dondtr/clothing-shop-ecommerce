<%-- Thông tin cá nhân (FR-04): sửa họ tên, giới tính, ngày sinh, số điện thoại; tên đăng nhập và email không đổi được. --%>
<c:set var="pageTitle" value="Thông tin cá nhân"/>
<c:set var="accountNav" value="profile"/>
<%-- Sau lỗi nhập liệu thì hiện lại giá trị khách vừa nhập --%>
<c:set var="v" value="${empty form ? profile : form}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Thông tin cá nhân</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row">
      <div class="col-md-3"><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></div>
      <div class="col-md-9">
        <h2 class="h4 text-black mb-3">Thông tin cá nhân</h2>
        <form action="${ctx}/account/profile" method="post" class="p-4 border" novalidate>
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <input type="hidden" name="version" value="${profile.version}">
          <c:if test="${not empty errors}"><div class="alert alert-danger">Vui lòng kiểm tra lại các ô được đánh dấu.</div></c:if>
          <div class="row">
            <div class="form-group col-md-6">
              <label class="text-black">Tên đăng nhập</label>
              <input type="text" class="form-control" value="${fn:escapeXml(profile.userName)}" disabled>
            </div>
            <div class="form-group col-md-6">
              <label class="text-black">Email</label>
              <input type="text" class="form-control" value="${fn:escapeXml(profile.email)}" disabled>
              <small class="text-muted">Tên đăng nhập và email không đổi được.</small>
            </div>
          </div>
          <div class="row">
            <t:field name="fullName" label="Họ và tên" value="${v.fullName}" required="true" maxlength="100"
                     error="${errors.fullName}" cssClass="col-md-6" autocomplete="name"/>
            <t:field name="phone" label="Số điện thoại" type="tel" value="${v.phone}" required="true" maxlength="15"
                     error="${errors.phone}" cssClass="col-md-6" autocomplete="tel"/>
          </div>
          <div class="row">
            <t:field name="gender" label="Giới tính" required="true" error="${errors.gender}" cssClass="col-md-6">
              <select id="gender" name="gender" class="form-control">
                <option value="FEMALE" ${v.gender == 'FEMALE' ? 'selected' : ''}>Nữ</option>
                <option value="MALE" ${v.gender == 'MALE' ? 'selected' : ''}>Nam</option>
                <option value="OTHER" ${v.gender == 'OTHER' ? 'selected' : ''}>Khác</option>
              </select>
            </t:field>
            <t:field name="dob" label="Ngày sinh" type="date" value="${v.dob}" required="true" error="${errors.dob}" cssClass="col-md-6"/>
          </div>
          <p class="text-muted small mb-3">
            Vai trò: <c:out value="${profile.role}"/>
            <c:if test="${not empty profile.level}"> · Hạng thành viên: <strong>${profile.level.label}</strong></c:if>
          </p>
          <button type="submit" class="btn btn-primary">Lưu thông tin</button>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
