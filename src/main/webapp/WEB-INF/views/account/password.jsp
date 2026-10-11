<%-- Đổi mật khẩu (FR-05). --%>
<c:set var="pageTitle" value="Đổi mật khẩu"/>
<c:set var="accountNav" value="password"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Đổi mật khẩu</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row">
      <div class="col-md-3"><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></div>
      <div class="col-md-9 col-lg-6">
        <h2 class="h4 text-black mb-3">Đổi mật khẩu</h2>
        <form action="${ctx}/account/password" method="post" class="p-4 border" novalidate>
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <t:field name="currentPassword" label="Mật khẩu hiện tại" type="password" required="true" maxlength="128"
                   error="${errors.currentPassword}" autocomplete="current-password"/>
          <t:field name="newPassword" label="Mật khẩu mới" type="password" required="true" maxlength="128"
                   error="${errors.newPassword}" hint="Ít nhất 8 ký tự" autocomplete="new-password"/>
          <t:field name="confirmPassword" label="Nhập lại mật khẩu mới" type="password" required="true" maxlength="128"
                   error="${errors.confirmPassword}" autocomplete="new-password"/>
          <button type="submit" class="btn btn-primary">Đổi mật khẩu</button>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
