<%-- Đăng nhập bằng tên đăng nhập hoặc email (FR-02). --%>
<c:set var="pageTitle" value="Đăng nhập"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <div class="row">
      <div class="col-md-12 mb-0"><a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Đăng nhập</strong></div>
    </div>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-6 col-lg-5">
        <h2 class="h3 mb-3 text-black">Đăng nhập</h2>
        <form action="${ctx}/login" method="post" class="p-3 p-lg-5 border">
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <input type="hidden" name="next" value="${fn:escapeXml(param.next)}">
          <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
          </c:if>
          <div class="form-group">
            <label for="login" class="text-black form-required">Tên đăng nhập hoặc email</label>
            <input type="text" class="form-control" id="login" name="login" required autofocus maxlength="100"
                   value="${fn:escapeXml(param.login)}" autocomplete="username">
          </div>
          <div class="form-group">
            <label for="password" class="text-black form-required">Mật khẩu</label>
            <input type="password" class="form-control" id="password" name="password" required maxlength="128"
                   autocomplete="current-password">
          </div>
          <div class="form-group mb-0">
            <button type="submit" class="btn btn-primary btn-lg btn-block">Đăng nhập</button>
          </div>
          <p class="mt-4 mb-0 text-center">Chưa có tài khoản? <a href="${ctx}/register">Đăng ký ngay</a></p>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
