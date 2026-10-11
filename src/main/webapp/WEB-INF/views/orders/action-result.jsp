<%-- Kết quả khi khách bấm nút trong email E1, E2 (FR-19, FR-20); không cần đăng nhập. --%>
<c:set var="pageTitle" value="${title}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="site-section">
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-8 text-center">
        <h2 class="h3 text-black mb-3"><c:out value="${title}"/></h2>
        <p class="lead"><c:out value="${message}"/></p>
        <c:if test="${not empty orderId}">
          <p><a href="${ctx}/orders/detail?id=${orderId}" class="btn btn-sm btn-primary">Xem đơn hàng #${orderId}</a></p>
        </c:if>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
