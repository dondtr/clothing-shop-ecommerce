<%-- Danh sách yêu thích (FR-10): theo mặt hàng; mặt hàng không còn bán vẫn nằm trong danh sách nhưng không mua được. --%>
<c:set var="pageTitle" value="Yêu thích"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <div class="row">
      <div class="col-md-12 mb-0"><a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Yêu thích</strong></div>
    </div>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <h2 class="h4 text-black mb-4">Danh sách yêu thích <small class="text-muted ml-1">(${fn:length(entries)} mặt hàng)</small></h2>
    <c:choose>
      <c:when test="${empty entries}">
        <div class="text-center py-5">
          <span class="icon-heart-o display-3 text-muted"></span>
          <p class="mt-3 mb-4">Chưa có mặt hàng nào. Bấm "Thêm vào danh sách yêu thích" ở trang sản phẩm để lưu lại.</p>
          <a href="${ctx}/shop" class="btn btn-primary btn-sm">Xem cửa hàng</a>
        </div>
      </c:when>
      <c:otherwise>
        <div class="row">
          <c:forEach items="${entries}" var="e">
            <c:set var="p" value="${e.product}"/>
            <div class="col-sm-6 col-lg-3 mb-4">
              <div class="block-4 text-center product-card border ${e.active ? '' : 'opacity-60'}">
                <c:if test="${e.active and p.onSale}"><span class="badge badge-danger badge-sale">-${f:number(p.discountPercent)}%</span></c:if>
                <c:choose>
                  <c:when test="${not e.active}"><span class="badge badge-dark badge-soldout">Ngừng bán</span></c:when>
                  <c:when test="${p.soldOut}"><span class="badge badge-secondary badge-soldout">Hết hàng</span></c:when>
                </c:choose>
                <figure class="block-4-image">
                  <c:choose>
                    <c:when test="${e.active}">
                      <a href="${ctx}/product?id=${p.id}"><img src="${fn:escapeXml(f:imageUrl(ctx, p.imgUrl))}" alt="${fn:escapeXml(p.name)}" class="img-fluid"></a>
                    </c:when>
                    <c:otherwise><img src="${fn:escapeXml(f:imageUrl(ctx, p.imgUrl))}" alt="${fn:escapeXml(p.name)}" class="img-fluid"></c:otherwise>
                  </c:choose>
                </figure>
                <div class="block-4-text p-3">
                  <h3 class="h6">
                    <c:choose>
                      <c:when test="${e.active}"><a href="${ctx}/product?id=${p.id}"><c:out value="${p.name}"/></a></c:when>
                      <c:otherwise><c:out value="${p.name}"/></c:otherwise>
                    </c:choose>
                  </h3>
                  <p class="text-primary font-weight-bold mb-2">
                    <c:if test="${p.onSale}"><span class="price-old">${f:vnd(p.basePrice)}</span></c:if>
                    ${f:vnd(p.currentPrice)}
                  </p>
                  <c:if test="${not e.active}"><p class="small text-muted mb-2">Mặt hàng không còn bán, không đặt mua được.</p></c:if>
                  <form action="${ctx}/wishlist/remove" method="post" class="m-0">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="productId" value="${p.id}">
                    <input type="hidden" name="from" value="wishlist">
                    <button type="submit" class="btn btn-link btn-sm p-0">Bỏ khỏi danh sách</button>
                  </form>
                </div>
              </div>
            </div>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
