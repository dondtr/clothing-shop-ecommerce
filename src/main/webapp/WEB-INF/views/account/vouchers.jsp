<%-- Voucher khách hàng dùng được ngay (FR-39): còn lượt, đang hiệu lực, dành cho mọi khách hàng hoặc đúng hạng. --%>
<c:set var="pageTitle" value="Voucher của tôi"/>
<c:set var="accountNav" value="vouchers"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Voucher của tôi</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row">
      <div class="col-md-3"><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></div>
      <div class="col-md-9">
        <h2 class="h4 text-black mb-3">Voucher bạn dùng được</h2>
        <c:if test="${empty vouchers}"><div class="alert alert-info">Hiện chưa có voucher nào dành cho bạn.</div></c:if>
        <div class="row">
          <c:forEach items="${vouchers}" var="vc">
            <div class="col-md-6 mb-3">
              <div class="border rounded p-3 h-100">
                <div class="d-flex justify-content-between">
                  <strong class="text-primary h5 mb-1"><c:out value="${vc.code}"/></strong>
                  <span class="badge badge-light border">${vc.eligibleCustomer.label}</span>
                </div>
                <p class="mb-1 text-black">
                  <c:choose>
                    <c:when test="${vc.discountType == 'PERCENT'}">Giảm ${f:number(vc.discountValue)}%<c:if test="${not empty vc.maxDiscount}">, tối đa ${f:vnd(vc.maxDiscount)}</c:if></c:when>
                    <c:otherwise>Giảm ${f:vnd(vc.discountValue)}</c:otherwise>
                  </c:choose>
                </p>
                <small class="text-muted d-block">Đơn từ ${f:vnd(vc.minOrderValue)} (tiền sản phẩm) · còn ${vc.quantity} lượt</small>
                <small class="text-muted d-block">Hiệu lực ${f:dateTime(vc.startDate)} – ${f:dateTime(vc.endDate)}</small>
              </div>
            </div>
          </c:forEach>
        </div>
        <p class="small text-muted">Mỗi đơn dùng tối đa một voucher; một mã dùng lại được ở nhiều đơn khi còn lượt và còn hiệu lực.</p>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
