<%-- Trang lỗi chung (web.xml). Không hiển thị chi tiết kỹ thuật cho người dùng. --%>
<%@ page isErrorPage="true" %>
<c:set var="status" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="pageTitle" value="Có lỗi xảy ra"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-8 col-lg-6 text-center">
        <h2 class="display-4 text-black"><c:out value="${status}"/></h2>
        <p class="lead">
          <c:choose>
            <c:when test="${status == 403}">Bạn không có quyền thực hiện thao tác này, hoặc phiên làm việc đã hết hạn. Hãy tải lại trang và thử lại.</c:when>
            <c:when test="${status == 404}">Không tìm thấy trang bạn yêu cầu (chức năng có thể chưa được làm).</c:when>
            <c:when test="${status == 405}">Thao tác không được hỗ trợ.</c:when>
            <c:otherwise>Đã có lỗi xảy ra. Vui lòng thử lại sau.</c:otherwise>
          </c:choose>
        </p>
        <a href="${ctx}/" class="btn btn-primary mt-3">Về trang chủ</a>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
