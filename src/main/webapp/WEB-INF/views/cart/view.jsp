<%-- Giỏ hàng (FR-09): đổi số lượng, xóa dòng, thành tiền theo giá sau giảm hiện tại. --%>
<c:set var="pageTitle" value="Giỏ hàng"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <div class="row">
      <div class="col-md-12 mb-0"><a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Giỏ hàng</strong></div>
    </div>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <c:choose>
      <c:when test="${empty cart.lines}">
        <div class="text-center py-5">
          <span class="icon-shopping_cart display-3 text-muted"></span>
          <h2 class="h4 text-black mt-3">Giỏ hàng đang trống</h2>
          <p class="mb-4">Hãy chọn vài món bạn thích nhé.</p>
          <a href="${ctx}/shop" class="btn btn-primary btn-sm">Tiếp tục mua sắm</a>
        </div>
      </c:when>
      <c:otherwise>
        <form action="${ctx}/cart/update" method="post">
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <div class="site-blocks-table mb-4">
            <table class="table table-bordered">
              <thead>
                <tr>
                  <th class="product-thumbnail">Ảnh</th>
                  <th class="product-name">Sản phẩm</th>
                  <th class="product-price">Đơn giá</th>
                  <th class="product-quantity">Số lượng</th>
                  <th class="product-total">Thành tiền</th>
                  <th class="product-remove">Xóa</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach items="${cart.lines}" var="line">
                  <tr class="${line.orderable ? '' : 'table-warning'}">
                    <td class="product-thumbnail">
                      <img src="${fn:escapeXml(f:imageUrl(ctx, line.imgUrl))}" alt="${fn:escapeXml(line.name)}" class="img-fluid">
                    </td>
                    <td class="product-name text-left">
                      <h2 class="h6 text-black mb-1">
                        <c:choose>
                          <c:when test="${line.status == 'NOT_ON_SALE'}"><c:out value="${line.name}"/></c:when>
                          <c:otherwise><a href="${ctx}/product?id=${line.productId}" class="text-black"><c:out value="${line.name}"/></a></c:otherwise>
                        </c:choose>
                      </h2>
                      <small class="text-muted">Màu <c:out value="${line.color}"/> · Kích thước <c:out value="${line.size}"/></small>
                      <c:choose>
                        <c:when test="${line.status == 'NOT_ON_SALE'}"><div class="text-danger small">Sản phẩm đã ngừng bán, không đặt được</div></c:when>
                        <c:when test="${line.status == 'OUT_OF_STOCK'}"><div class="text-danger small">Đã hết hàng</div></c:when>
                        <c:when test="${line.status == 'NOT_ENOUGH_STOCK'}"><div class="text-danger small">Chỉ còn ${line.stockQuantity} sản phẩm, vui lòng giảm số lượng</div></c:when>
                      </c:choose>
                    </td>
                    <td>
                      <c:if test="${line.onSale}"><span class="price-old d-block">${f:vnd(line.basePrice)}</span></c:if>
                      ${f:vnd(line.unitPrice)}
                    </td>
                    <td>
                      <c:choose>
                        <c:when test="${line.status == 'NOT_ON_SALE' or line.status == 'OUT_OF_STOCK'}">${line.quantity}</c:when>
                        <c:otherwise>
                          <div class="input-group mb-0 mx-auto" style="max-width: 130px;">
                            <div class="input-group-prepend">
                              <button class="btn btn-outline-primary js-btn-minus" type="button" aria-label="Giảm">&minus;</button>
                            </div>
                            <input type="number" class="form-control text-center" name="qty_${line.productId}" value="${line.quantity}"
                                   min="1" data-min="1" data-max="${line.stockQuantity}" aria-label="Số lượng">
                            <div class="input-group-append">
                              <button class="btn btn-outline-primary js-btn-plus" type="button" aria-label="Tăng">&plus;</button>
                            </div>
                          </div>
                        </c:otherwise>
                      </c:choose>
                    </td>
                    <td>${line.orderable ? f:vnd(line.subtotal) : '—'}</td>
                    <td>
                      <%-- Nút thuộc form xóa riêng bên dưới, để nhấn Enter ở ô số lượng không xóa nhầm dòng --%>
                      <button type="submit" form="remove-${line.productId}" class="btn btn-primary btn-sm"
                              title="Xóa khỏi giỏ hàng" aria-label="Xóa khỏi giỏ hàng">X</button>
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <div class="row">
            <div class="col-md-6 mb-4">
              <div class="row">
                <div class="col-sm-6 mb-3">
                  <button type="submit" class="btn btn-primary btn-sm btn-block">Cập nhật số lượng</button>
                </div>
                <div class="col-sm-6 mb-3">
                  <a href="${ctx}/shop" class="btn btn-outline-primary btn-sm btn-block">Tiếp tục mua sắm</a>
                </div>
              </div>
              <p class="small text-muted mb-0">Giỏ hàng dùng chung cho mọi trình duyệt bạn đang đăng nhập. Giá luôn theo giá hiện tại,
                chỉ được chốt khi đặt hàng; mã voucher nhập ở bước đặt hàng.</p>
            </div>
            <div class="col-md-6 pl-md-5">
              <div class="border-bottom mb-4 text-right"><h3 class="text-black h4 text-uppercase">Tổng giỏ hàng</h3></div>
              <div class="d-flex justify-content-between mb-2">
                <span class="text-black">Sản phẩm đặt được</span><strong class="text-black">${cart.orderableCount}/${fn:length(cart.lines)}</strong>
              </div>
              <div class="d-flex justify-content-between mb-3">
                <span class="text-black">Tiền hàng</span><strong class="text-black h5">${f:vnd(cart.total)}</strong>
              </div>
              <p class="small text-muted">Chưa gồm phí vận chuyển 20.000 ₫ và voucher. Dòng ngừng bán, hết hàng hoặc vượt tồn kho không được tính.</p>
              <c:if test="${cart.hasProblems}">
                <p class="small text-danger">Có sản phẩm cần điều chỉnh trước khi đặt hàng (đánh dấu màu vàng).</p>
              </c:if>
              <c:choose>
                <c:when test="${cart.orderableCount > 0}">
                  <a href="${ctx}/checkout" class="btn btn-primary btn-lg btn-block">Tiến hành đặt hàng</a>
                </c:when>
                <c:otherwise><button type="button" class="btn btn-secondary btn-lg btn-block" disabled>Tiến hành đặt hàng</button></c:otherwise>
              </c:choose>
            </div>
          </div>
        </form>
        <c:forEach items="${cart.lines}" var="line">
          <form id="remove-${line.productId}" action="${ctx}/cart/remove" method="post" class="d-none">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <input type="hidden" name="productId" value="${line.productId}">
          </form>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
