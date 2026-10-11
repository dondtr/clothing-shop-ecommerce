<%-- Chi tiết đơn hàng của khách hàng (FR-16): dòng hàng, địa chỉ, thanh toán, trạng thái; nút theo trạng thái. --%>
<c:set var="o" value="${order}"/>
<c:set var="pageTitle" value="Đơn hàng #${o.id}"/>
<c:set var="accountNav" value="orders"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <a href="${ctx}/orders">Đơn hàng của tôi</a>
    <span class="mx-2 mb-0">/</span> <strong class="text-black">#${o.id}</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row">
      <div class="col-md-3"><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></div>
      <div class="col-md-9">
        <div class="d-flex justify-content-between align-items-center flex-wrap mb-3">
          <h2 class="h4 text-black mb-0">Đơn hàng #${o.id}</h2>
          <t:status value="${o.status}"/>
        </div>
        <c:if test="${o.status != 'CANCELLED'}">
          <ul class="order-timeline mb-4">
            <li class="${o.status == 'PENDING' or o.status == 'CONFIRMED' or o.status == 'SHIPPING' or o.status == 'COMPLETED' ? 'done' : ''}">Chờ xác nhận</li>
            <li class="${o.status == 'CONFIRMED' or o.status == 'SHIPPING' or o.status == 'COMPLETED' ? 'done' : ''}">Đã xác nhận</li>
            <li class="${o.status == 'SHIPPING' or o.status == 'COMPLETED' ? 'done' : ''}">Đang giao</li>
            <li class="${o.status == 'COMPLETED' ? 'done' : ''}">Giao thành công</li>
          </ul>
        </c:if>

        <%@ include file="/WEB-INF/views/orders/order-summary.jspf" %>

        <div class="d-flex flex-wrap" style="gap: 8px;">
          <c:if test="${o.canConfirm}">
            <form action="${ctx}/orders/received" method="post" class="m-0">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="id" value="${o.id}">
              <button type="submit" class="btn btn-primary btn-sm">Đã nhận hàng</button>
            </form>
          </c:if>
          <c:if test="${o.canCancel}">
            <form action="${ctx}/orders/cancel" method="post" class="m-0"
                  onsubmit="return confirm('Hủy đơn hàng #${o.id}?${o.paymentStatus == 'PAID' ? ' Số tiền đã thanh toán sẽ được hoàn qua VNPAY.' : ''}');">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="id" value="${o.id}">
              <button type="submit" class="btn btn-outline-danger btn-sm">Hủy đơn</button>
            </form>
          </c:if>
        </div>
        <c:if test="${o.canRequestReturn}">
          <details class="mt-3 border rounded p-3">
            <summary class="text-primary">Yêu cầu trả hàng – hoàn tiền</summary>
            <p class="small text-muted mt-2 mb-2">Trả lại toàn bộ đơn trong 15 ngày kể từ khi nhận hàng (${f:dateTime(o.deliveredAt)}).
              Cửa hàng duyệt yêu cầu rồi hoàn tiền; mỗi đơn chỉ gửi được một lần.</p>
            <form action="${ctx}/orders/return" method="post" class="m-0">
              <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
              <input type="hidden" name="id" value="${o.id}">
              <textarea name="reason" class="form-control mb-2" rows="3" maxlength="500" required placeholder="Lý do trả hàng"></textarea>
              <button type="submit" class="btn btn-outline-primary btn-sm">Gửi yêu cầu trả hàng</button>
            </form>
          </details>
        </c:if>

        <c:if test="${o.status == 'COMPLETED'}">
          <h3 class="h5 text-black mt-4">Đánh giá sản phẩm</h3>
          <c:forEach items="${o.lines}" var="l">
            <div class="border rounded p-3 mb-2">
              <strong class="text-black"><c:out value="${l.name}"/></strong> <small class="text-muted">(<c:out value="${l.color}"/>, <c:out value="${l.size}"/>)</small>
              <c:choose>
                <c:when test="${not empty l.reviewId}">
                  <form action="${ctx}/reviews/delete" method="post" class="d-inline ml-2" onsubmit="return confirm('Xóa đánh giá của bạn cho sản phẩm này?');">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="reviewId" value="${l.reviewId}">
                    <input type="hidden" name="next" value="/orders/detail?id=${o.id}">
                    <span class="badge badge-success">Đã đánh giá</span>
                    <button type="submit" class="btn btn-link btn-sm p-0 ml-1">Xóa đánh giá</button>
                  </form>
                </c:when>
                <c:when test="${l.reviewable}">
                  <form action="${ctx}/reviews/create" method="post" class="mt-2">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="productId" value="${l.productId}">
                    <input type="hidden" name="next" value="/orders/detail?id=${o.id}">
                    <div class="star-input mb-2" role="radiogroup" aria-label="Số sao">
                      <c:forEach begin="1" end="5" var="k">
                        <c:set var="star" value="${6 - k}"/>
                        <input type="radio" id="star_${l.productId}_${star}" name="rating" value="${star}" required>
                        <label for="star_${l.productId}_${star}" title="${star} sao">★</label>
                      </c:forEach>
                    </div>
                    <textarea name="comment" class="form-control mb-2" rows="2" maxlength="2000" placeholder="Nhận xét (không bắt buộc)"></textarea>
                    <button type="submit" class="btn btn-primary btn-sm">Gửi đánh giá</button>
                  </form>
                </c:when>
              </c:choose>
            </div>
          </c:forEach>
        </c:if>

        <c:if test="${o.canConfirm}">
          <p class="small text-muted mt-2">Đơn tự hoàn tất sau 7 ngày kể từ khi bàn giao vận chuyển nếu bạn không xác nhận
            (không tự hoàn tất khi đang có báo chưa nhận được hàng).</p>
        </c:if>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
