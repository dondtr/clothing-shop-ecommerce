<%-- Tạo voucher thủ công (FR-35) hoặc sửa voucher chưa đến ngày bắt đầu (FR-36), có hộp thoại xác nhận (Q27). --%>
<c:set var="vc" value="${editing.voucher}"/>
<c:set var="isEdit" value="${not empty editing}"/>
<c:set var="pageTitle" value="${isEdit ? 'Sửa voucher' : 'Tạo voucher'}"/>
<c:set var="staffNav" value="vouchers"/>
<%-- Giá trị hiển thị: vừa gửi form thì lấy giá trị đã nhập, mở form sửa thì lấy từ voucher --%>
<c:set var="posted" value="${not empty param.discountType}"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<p class="mb-2"><a href="${ctx}/admin/vouchers">← Danh sách voucher</a></p>
<h1 class="h3 text-black mb-3">${isEdit ? 'Sửa voucher' : 'Tạo voucher'}</h1>
<c:if test="${isEdit}"><p class="text-muted small">Chỉ sửa được trước ngày bắt đầu (${f:dateTime(vc.startDate)}); mã voucher không đổi được.</p></c:if>
<div class="card"><div class="card-body">
  <form action="${ctx}/admin/vouchers/save" method="post" novalidate>
    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
    <c:if test="${isEdit}"><input type="hidden" name="editCode" value="${fn:escapeXml(vc.code)}"></c:if>
    <div class="row">
      <c:choose>
        <c:when test="${isEdit}">
          <div class="form-group col-md-4"><label class="text-black">Mã voucher</label>
            <input type="text" class="form-control" value="${fn:escapeXml(vc.code)}" disabled></div>
        </c:when>
        <c:otherwise>
          <t:field name="code" label="Mã voucher" value="${param.code}" required="true" maxlength="50" error="${errors.code}"
                   hint="Chữ cái không dấu, chữ số, dấu gạch ngang; lưu chữ in hoa" cssClass="col-md-4"/>
        </c:otherwise>
      </c:choose>
      <t:field name="discountType" label="Loại giảm giá" required="true" error="${errors.discountType}" cssClass="col-md-4">
        <select id="discountType" name="discountType" class="form-control">
          <c:forEach items="${types}" var="t"><option value="${t}" ${(posted ? param.discountType : vc.discountType) == t.name() ? 'selected' : ''}>${t.label}</option></c:forEach>
        </select>
      </t:field>
      <t:field name="discountValue" label="Giá trị giảm" type="number" step="any" required="true" error="${errors.discountValue}"
               value="${posted ? param.discountValue : (empty vc ? '' : vc.discountValue.stripTrailingZeros().toPlainString())}"
               hint="Phần trăm: 1 – 100; số tiền: bội số 1.000" cssClass="col-md-4"/>
    </div>
    <div class="row">
      <t:field name="quantity" label="Số lượng (lượt dùng)" type="number" min="0" required="true" error="${errors.quantity}"
               value="${posted ? param.quantity : (empty vc ? '100' : vc.quantity)}" cssClass="col-md-3"/>
      <t:field name="eligibleCustomer" label="Đối tượng khách hàng" required="true" error="${errors.eligibleCustomer}" cssClass="col-md-3">
        <select id="eligibleCustomer" name="eligibleCustomer" class="form-control">
          <c:forEach items="${eligibles}" var="e"><option value="${e}" ${(posted ? param.eligibleCustomer : (empty vc ? 'ALL' : vc.eligibleCustomer)) == e.name() ? 'selected' : ''}>${e.label}</option></c:forEach>
        </select>
      </t:field>
      <t:field name="minOrderValue" label="Đơn tối thiểu (₫)" type="number" min="0" step="1000" error="${errors.minOrderValue}"
               value="${posted ? param.minOrderValue : (empty vc ? '0' : vc.minOrderValue.stripTrailingZeros().toPlainString())}"
               hint="So với tiền sản phẩm" cssClass="col-md-3"/>
      <t:field name="maxDiscount" label="Giảm tối đa (₫)" type="number" min="1000" step="1000" error="${errors.maxDiscount}"
               value="${posted ? param.maxDiscount : (empty vc.maxDiscount ? '' : vc.maxDiscount.stripTrailingZeros().toPlainString())}"
               hint="Chỉ cho phần trăm; trống = không giới hạn" cssClass="col-md-3"/>
    </div>
    <div class="row">
      <t:field name="startDate" label="Bắt đầu" type="datetime-local" required="true" error="${errors.startDate}"
               value="${posted ? param.startDate : (empty vc ? '' : vc.startDate)}" cssClass="col-md-4"/>
      <t:field name="endDate" label="Kết thúc" type="datetime-local" required="true" error="${errors.endDate}"
               value="${posted ? param.endDate : (empty vc ? '' : vc.endDate)}" cssClass="col-md-4"/>
    </div>
    <button type="submit" class="btn btn-primary">Kiểm tra và xác nhận</button>

    <c:if test="${not empty confirmation}">
      <c:set var="cf" value="${confirmation}"/>
      <div class="modal fade" id="confirm-modal" tabindex="-1" role="dialog" aria-labelledby="confirm-title">
        <div class="modal-dialog" role="document"><div class="modal-content">
          <div class="modal-header"><h5 class="modal-title" id="confirm-title">Xác nhận ${isEdit ? 'sửa' : 'tạo'} voucher</h5></div>
          <div class="modal-body">
            <p class="mb-1">Mã: <strong><c:out value="${cf.code}"/></strong></p>
            <p class="mb-1">Giảm: <strong><c:choose><c:when test="${cf.discountType == 'PERCENT'}">${f:number(cf.discountValue)}%</c:when><c:otherwise>${f:vnd(cf.discountValue)}</c:otherwise></c:choose></strong>
              <c:if test="${not empty cf.maxDiscount}">, tối đa ${f:vnd(cf.maxDiscount)}</c:if></p>
            <p class="mb-1">Số lượng: <strong>${cf.quantity}</strong> · Đối tượng: <strong>${cf.eligibleCustomer.label}</strong></p>
            <p class="mb-1">Đơn tối thiểu: <strong>${f:vnd(cf.minOrderValue)}</strong></p>
            <p class="mb-1">Hiệu lực: <strong>${f:dateTime(cf.startDate)}</strong> đến <strong>${f:dateTime(cf.endDate)}</strong></p>
            <p class="small text-muted mb-0">Voucher không bao giờ bị xóa; còn sửa được đến trước ngày bắt đầu.</p>
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
