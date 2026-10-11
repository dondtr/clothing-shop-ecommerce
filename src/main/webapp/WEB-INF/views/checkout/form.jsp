<%-- Đặt hàng (FR-12, FR-14, FR-15): chọn dòng, địa chỉ, ghi chú, voucher, phương thức thanh toán; xem trước tổng tiền rồi xác nhận. --%>
<c:set var="pageTitle" value="Đặt hàng"/>
<c:set var="v" value="${checkout}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <a href="${ctx}/cart">Giỏ hàng</a>
    <span class="mx-2 mb-0">/</span> <strong class="text-black">Đặt hàng</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <c:if test="${not empty error}"><div class="alert alert-danger"><c:out value="${error}"/></div></c:if>
    <c:if test="${not empty errors}"><div class="alert alert-danger">Vui lòng kiểm tra lại các ô được đánh dấu.</div></c:if>
    <form id="checkout-form" action="${ctx}/checkout" method="post" novalidate>
      <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
      <input type="hidden" name="expectedTotal" value="${v.total}">
      <div class="row">
        <div class="col-lg-7 mb-5">
          <h2 class="h5 text-black mb-3">1. Sản phẩm</h2>
          <div class="border rounded mb-4">
            <c:if test="${empty v.lines}"><p class="p-3 mb-0">Giỏ hàng đang trống. <a href="${ctx}/shop">Tiếp tục mua sắm</a></p></c:if>
            <c:forEach items="${v.lines}" var="l">
              <label class="d-flex align-items-center p-3 border-bottom mb-0 ${l.orderable ? '' : 'text-muted'}" style="cursor: ${l.orderable ? 'pointer' : 'default'};">
                <input type="checkbox" name="productId" value="${l.productId}" class="mr-3" ${l.selected ? 'checked' : ''} ${l.orderable ? '' : 'disabled'}>
                <img src="${fn:escapeXml(f:imageUrl(ctx, l.imgUrl))}" alt="" style="width: 56px; height: 56px; object-fit: cover;" class="mr-3 rounded">
                <span class="flex-grow-1">
                  <strong class="text-black d-block"><c:out value="${l.name}"/></strong>
                  <small>Màu <c:out value="${l.color}"/> · Kích thước <c:out value="${l.size}"/> · SL ${l.quantity}</small>
                  <c:choose>
                    <c:when test="${l.status == 'NOT_ON_SALE'}"><small class="d-block text-danger">Đã ngừng bán, không đặt được</small></c:when>
                    <c:when test="${l.status == 'OUT_OF_STOCK'}"><small class="d-block text-danger">Đã hết hàng</small></c:when>
                    <c:when test="${l.status == 'NOT_ENOUGH_STOCK'}"><small class="d-block text-danger">Vượt tồn kho, hãy giảm số lượng trong giỏ</small></c:when>
                  </c:choose>
                </span>
                <span class="text-right">${f:vnd(l.subtotal)}<small class="d-block text-muted">${f:vnd(l.unitPrice)} × ${l.quantity}</small></span>
              </label>
            </c:forEach>
          </div>

          <h2 class="h5 text-black mb-3">2. Địa chỉ nhận hàng</h2>
          <div class="border rounded p-3 mb-4">
            <c:forEach items="${v.addresses}" var="a">
              <div class="form-check mb-2">
                <input class="form-check-input" type="radio" name="addressChoice" id="addr_${a.id}" value="${a.id}" ${v.addressId == a.id ? 'checked' : ''}>
                <label class="form-check-label" for="addr_${a.id}"><c:out value="${a.text}"/>
                  <c:if test="${a.defaultAddress}"><span class="badge badge-primary ml-1">Mặc định</span></c:if></label>
              </div>
            </c:forEach>
            <div class="form-check mb-2">
              <input class="form-check-input" type="radio" name="addressChoice" id="addr_new" value="new" ${empty v.addressId ? 'checked' : ''}>
              <label class="form-check-label" for="addr_new">Địa chỉ mới (được lưu vào sổ địa chỉ)</label>
            </div>
            <div id="new-address" class="row pl-4 ${empty v.addressId ? '' : 'd-none'}">
              <t:field name="street" label="Số nhà, tên đường" value="${v.newAddress.street}" required="true" maxlength="255" error="${errors.street}" cssClass="col-md-12"/>
              <t:field name="ward" label="Phường/xã" value="${v.newAddress.ward}" required="true" maxlength="100" error="${errors.ward}" cssClass="col-md-6"/>
              <t:field name="province" label="Tỉnh/thành phố" value="${v.newAddress.province}" required="true" maxlength="100" error="${errors.province}" cssClass="col-md-6"/>
            </div>
            <t:field name="note" label="Ghi chú cho cửa hàng (không bắt buộc)" error="${errors.note}">
              <textarea id="note" name="note" class="form-control" rows="2" maxlength="500"><c:out value="${v.note}"/></textarea>
            </t:field>
          </div>

          <h2 class="h5 text-black mb-3">3. Thanh toán</h2>
          <div class="border rounded p-3">
            <div class="form-check mb-2">
              <input class="form-check-input" type="radio" name="method" id="m_cod" value="COD" ${v.method == 'COD' ? 'checked' : ''}>
              <label class="form-check-label" for="m_cod">Thanh toán khi nhận hàng (COD)</label>
            </div>
            <div class="form-check">
              <input class="form-check-input" type="radio" name="method" id="m_vnpay" value="VNPAY" ${v.method == 'VNPAY' ? 'checked' : ''} ${v.vnpayEnabled ? '' : 'disabled'}>
              <label class="form-check-label" for="m_vnpay">Thanh toán trực tuyến qua VNPAY
                <c:if test="${not v.vnpayEnabled}"><small class="text-muted">(chưa cấu hình trên máy chủ này)</small></c:if></label>
            </div>
          </div>
        </div>

        <div class="col-lg-5">
          <div class="border rounded p-4 bg-light">
            <h2 class="h5 text-black mb-3">Đơn hàng</h2>
            <p class="mb-3 small">Người nhận: <strong><c:out value="${v.recipientName}"/></strong> · <c:out value="${v.recipientPhone}"/>
              <br><span class="text-muted">Người nhận luôn là chủ tài khoản; đổi tên, số điện thoại ở <a href="${ctx}/account/profile">Thông tin cá nhân</a>.</span></p>
            <div class="form-group">
              <label for="voucherCode" class="text-black">Mã voucher</label>
              <div class="input-group">
                <input type="text" id="voucherCode" name="voucherCode" class="form-control ${not empty v.voucherError ? 'is-invalid' : ''}"
                       value="${fn:escapeXml(v.voucherCode)}" maxlength="50" placeholder="Nhập mã nếu có">
                <div class="input-group-append"><button type="submit" name="action" value="preview" class="btn btn-outline-primary">Áp dụng</button></div>
              </div>
              <c:choose>
                <c:when test="${not empty v.voucherError}"><div class="invalid-feedback d-block"><c:out value="${v.voucherError}"/></div></c:when>
                <c:when test="${not empty v.voucherCode}"><small class="text-success">Đã áp dụng voucher <c:out value="${v.voucherCode}"/></small></c:when>
                <c:otherwise><small><a href="${ctx}/vouchers" target="_blank">Xem voucher bạn dùng được</a></small></c:otherwise>
              </c:choose>
            </div>
            <table class="table table-sm mb-3">
              <tr><td>Tiền sản phẩm</td><td class="text-right">${f:vnd(v.productAmount)}</td></tr>
              <tr><td>Giảm giá voucher</td><td class="text-right">−${f:vnd(v.discount)}</td></tr>
              <tr><td>Phí vận chuyển</td><td class="text-right">${f:vnd(v.shippingFee)}</td></tr>
              <tr class="h5"><td class="text-black">Tổng tiền</td><td class="text-right text-primary"><strong>${f:vnd(v.total)}</strong></td></tr>
            </table>
            <c:forEach items="${v.problems}" var="p"><p class="small text-danger mb-1"><c:out value="${p}"/></p></c:forEach>
            <p id="recalc-hint" class="small text-warning d-none">Bạn vừa thay đổi lựa chọn; bấm "Cập nhật tổng tiền" để tính lại trước khi xác nhận.</p>
            <button type="submit" name="action" value="preview" class="btn btn-outline-primary btn-block">Cập nhật tổng tiền</button>
            <button type="submit" id="place-order" name="action" value="place" class="btn btn-primary btn-lg btn-block mt-2" ${v.canPlace ? '' : 'disabled'}>
              Xác nhận mua hàng</button>
            <p class="small text-muted mt-2 mb-0">Đơn COD được tạo ngay khi xác nhận. Cửa hàng không gửi email xác nhận đặt hàng; bạn theo dõi đơn ở mục Đơn hàng của tôi.</p>
          </div>
        </div>
      </div>
    </form>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
<script>
  (function ($) {
    $('input[name=addressChoice]').on('change', function () {
      $('#new-address').toggleClass('d-none', !$('#addr_new').is(':checked'));
    });
    // Tổng tiền hiển thị chỉ đúng với lựa chọn đã tính; đổi lựa chọn thì phải tính lại trước khi xác nhận
    $('#checkout-form').on('change input', 'input[name=productId], input[name=method], #voucherCode', function () {
      $('#place-order').prop('disabled', true);
      $('#recalc-hint').removeClass('d-none');
    });
  })(jQuery);
</script>
</body>
</html>
