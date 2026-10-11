<%-- Chi tiết mặt hàng (FR-08): thông tin chung, chọn màu + kích thước để xem ảnh và tồn kho của từng dòng, đánh giá. --%>
<c:set var="pageTitle" value="${product.name}"/>
<c:set var="activeNav" value="shop"/>
<c:set var="selected" value="${product.selectedVariant}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <div class="row">
      <div class="col-md-12 mb-0">
        <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span>
        <a href="${ctx}/shop">Cửa hàng</a> <span class="mx-2 mb-0">/</span>
        <a href="${ctx}/shop?category=${product.category}"><c:out value="${product.category.label}"/></a> <span class="mx-2 mb-0">/</span>
        <strong class="text-black"><c:out value="${product.name}"/></strong>
      </div>
    </div>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row">
      <div class="col-md-6 mb-4">
        <img id="variant-image" src="${fn:escapeXml(f:imageUrl(ctx, selected.imgUrl))}" alt="${fn:escapeXml(product.name)}"
             class="img-fluid rounded product-image">
      </div>
      <div class="col-md-6">
        <h2 class="text-black"><c:out value="${product.name}"/></h2>
        <p class="mb-2 text-muted"><c:out value="${product.category.label}"/></p>
        <p class="mb-2">
          <c:choose>
            <c:when test="${not empty product.averageRating}">
              <a href="#reviews" class="text-reset">
                <span class="rating-stars">${f:stars(product.averageRating)}</span>
                <span class="text-muted">${f:number(product.averageRating)}/5 · ${fn:length(product.reviews)} đánh giá</span>
              </a>
            </c:when>
            <c:otherwise><span class="text-muted">Chưa có đánh giá</span></c:otherwise>
          </c:choose>
        </p>
        <p class="mb-3">
          <strong class="text-primary h4">${f:vnd(product.currentPrice)}</strong>
          <c:if test="${product.onSale}">
            <span class="price-old ml-2">${f:vnd(product.basePrice)}</span>
            <span class="badge badge-danger ml-1">-${f:number(product.discountPercent)}%</span>
          </c:if>
        </p>
        <c:if test="${not empty product.description}">
          <p class="text-pre-line"><c:out value="${product.description}"/></p>
        </c:if>

        <%-- Dữ liệu các dòng cho script chọn biến thể bên dưới --%>
        <ul id="variant-data" class="d-none">
          <c:forEach items="${product.variants}" var="v">
            <li data-id="${v.id}" data-color="${fn:escapeXml(v.color)}" data-size="${fn:escapeXml(v.size)}"
                data-img="${fn:escapeXml(f:imageUrl(ctx, v.imgUrl))}" data-stock="${v.stockQuantity}"></li>
          </c:forEach>
        </ul>

        <div class="mb-3">
          <h3 class="h6 text-black mb-2">Màu sắc</h3>
          <div class="variant-options">
            <c:forEach items="${product.colors}" var="col" varStatus="st">
              <input type="radio" name="pickColor" id="color_${st.index}" value="${fn:escapeXml(col)}" ${col == selected.color ? 'checked' : ''}>
              <label for="color_${st.index}"><c:out value="${col}"/></label>
            </c:forEach>
          </div>
        </div>
        <div class="mb-3">
          <h3 class="h6 text-black mb-2">Kích thước</h3>
          <div class="variant-options">
            <c:forEach items="${product.sizes}" var="s" varStatus="st">
              <input type="radio" name="pickSize" id="size_${st.index}" value="${fn:escapeXml(s)}" ${s == selected.size ? 'checked' : ''}>
              <label for="size_${st.index}"><c:out value="${s}"/></label>
            </c:forEach>
          </div>
        </div>
        <p id="stock-info" class="mb-3 ${selected.stockQuantity == 0 ? 'text-danger' : 'text-muted'}">
          <c:choose><c:when test="${selected.stockQuantity == 0}">Hết hàng</c:when><c:otherwise>Còn ${selected.stockQuantity} sản phẩm</c:otherwise></c:choose>
        </p>

        <c:choose>
          <c:when test="${empty sessionScope.auth}">
            <c:url var="loginUrl" value="/login"><c:param name="next" value="/product?id=${selected.id}"/></c:url>
            <p><a href="${fn:escapeXml(loginUrl)}" class="btn btn-sm btn-outline-primary">Đăng nhập để mua hàng</a></p>
          </c:when>
          <c:when test="${sessionScope.auth.employee}">
            <p class="text-muted small">Tài khoản nhân viên không mua hàng; vui lòng dùng tài khoản khách hàng riêng (BR-27).</p>
          </c:when>
          <c:otherwise>
            <%-- Thêm vào giỏ hàng (FR-09): chỉ khách hàng đã đăng nhập (Q40); dòng hết hàng không thêm được --%>
            <form action="${ctx}/cart/add" method="post" class="mb-3">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="productId" id="variant-id" value="${selected.id}">
              <div class="d-flex flex-wrap align-items-center" style="gap: 12px;">
                <div class="input-group" style="max-width: 140px;">
                  <div class="input-group-prepend">
                    <button class="btn btn-outline-primary js-btn-minus" type="button" aria-label="Giảm">&minus;</button>
                  </div>
                  <input type="number" id="quantity" name="quantity" class="form-control text-center" value="1" min="1"
                         data-min="1" data-max="${selected.stockQuantity}" aria-label="Số lượng">
                  <div class="input-group-append">
                    <button class="btn btn-outline-primary js-btn-plus" type="button" aria-label="Tăng">&plus;</button>
                  </div>
                </div>
                <button type="submit" id="add-to-cart" class="btn btn-sm btn-primary" ${selected.stockQuantity == 0 ? 'disabled' : ''}>
                  Thêm vào giỏ hàng
                </button>
              </div>
            </form>
            <%-- Yêu thích (FR-10): lưu dòng đang xem; mỗi mặt hàng chỉ một lần trong danh sách --%>
            <form action="${ctx}/wishlist/${inWishlist ? 'remove' : 'add'}" method="post" class="mb-3">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="productId" id="wishlist-product-id" value="${selected.id}">
              <button type="submit" class="btn btn-link p-0">
                <span class="icon ${inWishlist ? 'icon-heart text-danger' : 'icon-heart-o'}"></span>
                ${inWishlist ? 'Bỏ khỏi danh sách yêu thích' : 'Thêm vào danh sách yêu thích'}
              </button>
            </form>
          </c:otherwise>
        </c:choose>
      </div>
    </div>

    <div class="row mt-5" id="reviews">
      <div class="col-md-12">
        <h3 class="h4 text-black mb-3">Đánh giá sản phẩm</h3>
        <c:if test="${empty product.reviews}">
          <p class="text-muted">Chưa có đánh giá nào. Khách hàng đã nhận hàng có thể đánh giá sau khi đơn hoàn tất.</p>
        </c:if>
        <c:forEach items="${product.reviews}" var="r">
          <div class="review-item">
            <div class="d-flex justify-content-between">
              <strong class="text-black"><c:out value="${r.authorName}"/></strong>
              <small class="text-muted">${f:dateTime(r.createdAt)}
                <c:if test="${sessionScope.auth.customer and sessionScope.auth.id == r.customerId}">
                  <form action="${ctx}/reviews/delete" method="post" class="d-inline ml-2" onsubmit="return confirm('Xóa đánh giá của bạn?');">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="reviewId" value="${r.id}">
                    <input type="hidden" name="next" value="/product?id=${selected.id}">
                    <button type="submit" class="btn btn-link btn-sm p-0">Xóa</button>
                  </form>
                </c:if></small>
            </div>
            <div class="rating-stars" aria-label="${r.rating} trên 5 sao">${f:stars(r.rating)}</div>
            <c:if test="${not empty r.comment}"><p class="mb-0 text-pre-line"><c:out value="${r.comment}"/></p></c:if>
          </div>
        </c:forEach>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
<script>
  // Chọn màu + kích thước để xem ảnh và tồn kho của dòng tương ứng (FR-08 bước 2).
  (function ($) {
    var variants = $('#variant-data li').map(function () {
      var li = $(this);
      return { id: String(li.attr('data-id')), color: li.attr('data-color'), size: li.attr('data-size'),
               img: li.attr('data-img'), stock: parseInt(li.attr('data-stock'), 10) };
    }).get();
    function find(color, size) {
      return variants.filter(function (v) { return v.color === color && v.size === size; })[0];
    }
    function update() {
      var color = $('input[name=pickColor]:checked').val();
      var size = $('input[name=pickSize]:checked').val();
      // Kích thước không có trong màu đang chọn thì không chọn được
      $('input[name=pickSize]').each(function () {
        $(this).prop('disabled', !!color && !find(color, $(this).val()));
      });
      var v = color && size ? find(color, size) : null;
      var info = $('#stock-info').removeClass('text-danger text-muted');
      if (!v) {
        info.addClass('text-muted').text(color && size ? 'Không có màu và kích thước này' : 'Vui lòng chọn màu và kích thước');
      } else if (v.stock <= 0) {
        info.addClass('text-danger').text('Hết hàng');
      } else {
        info.addClass('text-muted').text('Còn ' + v.stock + ' sản phẩm');
      }
      if (v) { $('#variant-image').attr('src', v.img); }
      $(document).trigger('variant:change', [v || null]);
    }
    // Form thêm vào giỏ (chỉ có khi khách hàng đã đăng nhập) luôn gửi đúng dòng đang chọn
    $(document).on('variant:change', function (e, v) {
      var ok = !!v && v.stock > 0;
      $('#variant-id').val(v ? v.id : '');
      if (v) { $('#wishlist-product-id').val(v.id); }
      $('#add-to-cart').prop('disabled', !ok);
      var qty = $('#quantity');
      qty.data('max', ok ? v.stock : 1);
      if (ok && parseInt(qty.val(), 10) > v.stock) { qty.val(v.stock); }
    });
    $('input[name=pickColor], input[name=pickSize]').on('change', update);
    update();
  })(jQuery);
</script>
</body>
</html>
