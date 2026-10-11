<%-- Giảm giá sản phẩm (FR-34, quản lý): các đợt giảm giá, tạo đợt mới và sửa đợt chưa bắt đầu, có hộp thoại xác nhận (Q68). --%>
<c:set var="p" value="${product}"/>
<c:set var="pageTitle" value="Giảm giá – ${p.name}"/>
<c:set var="staffNav" value="products"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<p class="mb-2"><a href="${ctx}/staff/products/edit?id=${p.firstId}">← <c:out value="${p.name}"/></a></p>
<h1 class="h3 text-black mb-2">Giảm giá: <c:out value="${p.name}"/></h1>
<p class="text-muted small mb-4">Giảm theo phần trăm giá bán (${f:vnd(p.basePrice)}) cho mọi biến thể; giá sau giảm làm tròn đến hàng nghìn. Các đợt không được trùng thời gian.
  Đợt chưa đến ngày bắt đầu còn sửa được; đã bắt đầu thì chỉ xem; không có chức năng xóa, đợt đã kết thúc giữ làm lịch sử.</p>

<div class="card mb-4"><div class="card-body p-0">
  <table class="table table-sm mb-0">
    <thead class="thead-light"><tr><th>Mức giảm</th><th>Bắt đầu</th><th>Kết thúc</th><th>Tình trạng</th><th></th></tr></thead>
    <tbody>
      <c:if test="${empty p.campaigns}"><tr><td colspan="5" class="text-center text-muted py-3">Chưa có đợt giảm giá.</td></tr></c:if>
      <c:forEach items="${p.campaigns}" var="cp">
        <tr>
          <td>${f:number(cp.discountValue)}%</td>
          <td>${f:dateTime(cp.startDate)}</td>
          <td>${f:dateTime(cp.endDate)}</td>
          <td>${cp.state}</td>
          <td class="text-right"><c:if test="${cp.editable}"><a href="${ctx}/admin/discounts?id=${p.firstId}&amp;campaign=${cp.startDate}">Sửa</a></c:if></td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div></div>

<%-- Đang sửa một đợt (chọn từ bảng) hay tạo đợt mới --%>
<c:set var="editing" value="${not empty param.campaign}"/>
<c:forEach items="${p.campaigns}" var="cp"><c:if test="${editing and cp.startDate == param.campaign}"><c:set var="current" value="${cp}"/></c:if></c:forEach>
<div class="card"><div class="card-body">
  <h2 class="h5 text-black">${editing ? 'Sửa đợt giảm giá' : 'Tạo đợt giảm giá'}</h2>
  <form id="discount-form" action="${ctx}/admin/discounts/save" method="post" novalidate>
    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
    <input type="hidden" name="id" value="${p.firstId}">
    <input type="hidden" name="campaign" value="${fn:escapeXml(param.campaign)}">
    <div class="row">
      <t:field name="discountValue" label="Mức giảm (%)" type="number" min="0.01" max="100" step="0.01" required="true"
               value="${not empty param.discountValue ? param.discountValue : (empty current ? '' : current.discountValue.stripTrailingZeros().toPlainString())}"
               error="${errors.discountValue}" cssClass="col-md-3"/>
      <t:field name="startDate" label="Bắt đầu" type="datetime-local" required="true"
               value="${not empty param.startDate ? param.startDate : (empty current ? '' : current.startDate)}" error="${errors.startDate}" cssClass="col-md-4"/>
      <t:field name="endDate" label="Kết thúc" type="datetime-local" required="true"
               value="${not empty param.endDate ? param.endDate : (empty current ? '' : current.endDate)}" error="${errors.endDate}" cssClass="col-md-4"/>
    </div>
    <button type="submit" class="btn btn-primary">Kiểm tra và xác nhận</button>
    <c:if test="${editing}"><a href="${ctx}/admin/discounts?id=${p.firstId}" class="btn btn-link">Tạo đợt mới thay vì sửa</a></c:if>

    <c:if test="${not empty confirmation}">
      <%-- Hộp thoại xác nhận kèm toàn bộ thông tin (FR-34 bước 3); bấm Sửa lại để tiếp tục chỉnh trên form --%>
      <div class="modal fade" id="confirm-modal" tabindex="-1" role="dialog" aria-labelledby="confirm-title">
        <div class="modal-dialog" role="document"><div class="modal-content">
          <div class="modal-header"><h5 class="modal-title" id="confirm-title">Xác nhận ${editing ? 'sửa' : 'tạo'} đợt giảm giá</h5></div>
          <div class="modal-body">
            <p class="mb-1">Mặt hàng: <strong><c:out value="${p.name}"/></strong> (${fn:length(p.rows)} biến thể)</p>
            <p class="mb-1">Mức giảm: <strong>${f:number(confirmation.discountValue)}%</strong></p>
            <p class="mb-1">Thời gian: <strong>${f:dateTime(confirmation.startDate)}</strong> đến <strong>${f:dateTime(confirmation.endDate)}</strong></p>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline-secondary" data-dismiss="modal">Sửa lại</button>
            <button type="submit" name="confirm" value="1" class="btn btn-primary">Xác nhận</button>
          </div>
        </div></div>
      </div>
    </c:if>
  </form>
</div></div>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
<c:if test="${not empty confirmation}"><script>$('#confirm-modal').modal('show');</script></c:if>
</body>
</html>
