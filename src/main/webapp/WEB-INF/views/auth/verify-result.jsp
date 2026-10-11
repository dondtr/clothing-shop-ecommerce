<%-- Kết quả bấm link xác thực email (FR-01 bước 4 – 5). --%>
<c:set var="pageTitle" value="Xác thực email"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-8 col-lg-6 text-center">
        <c:choose>
          <c:when test="${empty error}">
            <span class="icon-check_circle display-3 text-success"></span>
            <h2 class="h3 mt-3 mb-3 text-black">Đăng ký thành công</h2>
            <p>Tài khoản <strong><c:out value="${verifiedUserName}"/></strong> đã được tạo. Bạn có thể đăng nhập ngay.</p>
            <a href="${ctx}/login" class="btn btn-primary mt-3">Đăng nhập</a>
          </c:when>
          <c:otherwise>
            <span class="icon-error_outline display-3 text-danger"></span>
            <h2 class="h3 mt-3 mb-3 text-black">Không xác thực được</h2>
            <p><c:out value="${error}"/></p>
            <a href="${ctx}/register" class="btn btn-primary mt-3">Đăng ký lại</a>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
