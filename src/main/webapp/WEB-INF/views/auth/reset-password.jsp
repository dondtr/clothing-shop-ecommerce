<%-- Đặt mật khẩu mới từ link trong email E9 (FR-03 bước 3). --%>
<c:set var="pageTitle" value="Đặt lại mật khẩu"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Đặt lại mật khẩu</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-6 col-lg-5">
        <h2 class="h3 mb-3 text-black">Đặt lại mật khẩu</h2>
        <c:choose>
          <c:when test="${not usable}">
            <div class="alert alert-danger">Link đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được dùng.</div>
            <a href="${ctx}/forgot-password" class="btn btn-primary btn-sm">Yêu cầu link mới</a>
          </c:when>
          <c:otherwise>
            <form action="${ctx}/reset-password" method="post" class="p-3 p-lg-5 border" novalidate>
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="token" value="${fn:escapeXml(param.token)}">
              <t:field name="password" label="Mật khẩu mới" type="password" required="true" maxlength="128"
                       error="${errors.password}" hint="Ít nhất 8 ký tự" autocomplete="new-password"/>
              <t:field name="confirmPassword" label="Nhập lại mật khẩu mới" type="password" required="true" maxlength="128"
                       error="${errors.confirmPassword}" autocomplete="new-password"/>
              <button type="submit" class="btn btn-primary btn-block">Lưu mật khẩu mới</button>
            </form>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
