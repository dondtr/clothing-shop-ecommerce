<%-- Quên mật khẩu (FR-03 bước 1 – 2): luôn báo cùng một câu, không cho biết email có tài khoản hay không. --%>
<c:set var="pageTitle" value="Quên mật khẩu"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <a href="${ctx}/login">Đăng nhập</a>
    <span class="mx-2 mb-0">/</span> <strong class="text-black">Quên mật khẩu</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-6 col-lg-5">
        <h2 class="h3 mb-3 text-black">Quên mật khẩu</h2>
        <c:choose>
          <c:when test="${not empty sent}">
            <div class="alert alert-success"><c:out value="${sent}"/> Link có hiệu lực trong 15 phút.</div>
            <a href="${ctx}/login" class="btn btn-outline-primary btn-sm">Quay lại đăng nhập</a>
          </c:when>
          <c:otherwise>
            <form action="${ctx}/forgot-password" method="post" class="p-3 p-lg-5 border">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <p class="text-muted">Nhập email của tài khoản. Hệ thống sẽ gửi link để bạn đặt mật khẩu mới.</p>
              <t:field name="email" label="Email" type="email" required="true" maxlength="100" autocomplete="email"/>
              <button type="submit" class="btn btn-primary btn-block">Gửi link đặt lại mật khẩu</button>
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
