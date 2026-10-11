<%-- Báo "Chưa thấy hàng đâu" đang chờ xử lý (FR-42 bước 3) và xử lý: giao lại hoặc đã giải quyết (FR-21). --%>
<c:set var="pageTitle" value="Sự cố giao hàng"/>
<c:set var="staffNav" value="issues"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<h1 class="h3 text-black mb-2">Sự cố giao hàng</h1>
<p class="text-muted small mb-4">Khách hàng bấm "Chưa thấy hàng đâu" trong email nhắc. Đơn vẫn đang giao và không tự hoàn tất cho đến khi xử lý.
  Lý do và cách giải quyết được gửi trong email cho khách, không lưu lại. Giao lại không thu thêm phí vận chuyển; hàng cũ bị trả về thì cộng tồn kho ở trang Sản phẩm.</p>
<c:if test="${empty issues}"><div class="alert alert-success">Không có sự cố nào đang chờ xử lý.</div></c:if>
<c:forEach items="${issues}" var="i">
  <c:set var="mine" value="${formIssueId == i.issueId}"/>
  <div class="card mb-3"><div class="card-body">
    <div class="d-flex justify-content-between flex-wrap">
      <h2 class="h6 mb-1"><a href="${ctx}/staff/orders/detail?id=${i.orderId}">Đơn #${i.orderId}</a> ·
        <a href="${ctx}/staff/customers/detail?id=${i.customerId}"><c:out value="${i.customerName}"/></a>
        <small class="text-muted">· <c:out value="${i.customerPhone}"/> · <c:out value="${i.customerEmail}"/></small></h2>
      <small class="text-muted">Báo lúc ${f:dateTime(i.reportedAt)} · bàn giao vận chuyển ${f:dateTime(i.shippedAt)}</small>
    </div>
    <ul class="small mb-3">
      <c:forEach items="${i.lines}" var="l">
        <li><c:out value="${l.name}"/> (<c:out value="${l.color}"/>, <c:out value="${l.size}"/>) × ${l.quantity}
          <span class="${l.stockQuantity < l.quantity ? 'text-danger' : 'text-muted'}">· tồn kho ${l.stockQuantity}</span></li>
      </c:forEach>
    </ul>
    <form action="${ctx}/staff/delivery-issues/resolve" method="post" novalidate>
      <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
      <input type="hidden" name="issueId" value="${i.issueId}">
      <div class="row">
        <t:field name="reason" label="Lý do" value="${mine ? param.reason : ''}" required="true" maxlength="500"
                 error="${mine ? errors.reason : ''}" cssClass="col-md-6" placeholder="Ví dụ: đơn vị vận chuyển giao nhầm địa chỉ"/>
        <t:field name="solution" label="Cách cửa hàng giải quyết" value="${mine ? param.solution : ''}" required="true" maxlength="500"
                 error="${mine ? errors.solution : ''}" cssClass="col-md-6" placeholder="Ví dụ: gửi lại hàng trong 2 ngày"/>
      </div>
      <div class="d-flex flex-wrap" style="gap: 8px;">
        <button type="submit" name="resolution" value="RESHIP" class="btn btn-sm btn-primary" ${i.reshipPossible ? '' : 'disabled'}>Giao lại hàng</button>
        <button type="submit" name="resolution" value="RESOLVED" class="btn btn-sm btn-outline-primary">Đã giải quyết (đã liên hệ vận chuyển)</button>
        <a href="${ctx}/staff/orders/detail?id=${i.orderId}" class="btn btn-sm btn-outline-danger">Giao không thành công – hủy đơn</a>
      </div>
      <c:if test="${not i.reshipPossible}"><small class="text-danger d-block mt-1">Không đủ tồn kho để giao lại (Q54).</small></c:if>
    </form>
  </div></div>
</c:forEach>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
