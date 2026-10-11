<%-- Voucher đã ban hành (FR-36): lọc theo mã, đối tượng, tình trạng; sửa voucher thủ công chưa đến ngày bắt đầu. --%>
<c:set var="pageTitle" value="Voucher"/>
<c:set var="staffNav" value="vouchers"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<div class="d-flex justify-content-between align-items-center flex-wrap mb-3">
  <h1 class="h3 text-black mb-0">Voucher</h1>
  <div><a href="${ctx}/admin/voucher-settings" class="btn btn-sm btn-light border">Voucher tự động</a>
    <a href="${ctx}/admin/vouchers/new" class="btn btn-sm btn-primary">+ Tạo voucher</a></div>
</div>
<form action="${ctx}/admin/vouchers" method="get" class="form-inline flex-wrap mb-3" style="gap: 6px;">
  <input type="text" name="q" value="${fn:escapeXml(param.q)}" class="form-control form-control-sm" placeholder="Mã voucher" maxlength="50">
  <select name="eligible" class="form-control form-control-sm" aria-label="Đối tượng">
    <option value="">Mọi đối tượng</option>
    <c:forEach items="${eligibles}" var="e"><option value="${e}" ${param.eligible == e.name() ? 'selected' : ''}>${e.label}</option></c:forEach>
  </select>
  <select name="state" class="form-control form-control-sm" aria-label="Tình trạng">
    <option value="">Mọi tình trạng</option>
    <option value="UPCOMING" ${param.state == 'UPCOMING' ? 'selected' : ''}>Chưa đến hạn</option>
    <option value="ACTIVE" ${param.state == 'ACTIVE' ? 'selected' : ''}>Đang hiệu lực</option>
    <option value="USED_UP" ${param.state == 'USED_UP' ? 'selected' : ''}>Hết lượt</option>
    <option value="EXPIRED" ${param.state == 'EXPIRED' ? 'selected' : ''}>Hết hạn</option>
  </select>
  <button type="submit" class="btn btn-sm btn-outline-primary">Lọc</button>
</form>
<div class="card"><div class="card-body p-0">
  <table class="table table-sm table-hover mb-0">
    <thead class="thead-light"><tr><th>Mã</th><th>Giảm</th><th>Đối tượng</th><th class="text-right">Đơn từ</th><th class="text-center">Còn lượt</th><th>Hiệu lực</th><th>Tình trạng</th><th></th></tr></thead>
    <tbody>
      <c:if test="${empty vouchers}"><tr><td colspan="8" class="text-center text-muted py-3">Không có voucher phù hợp.</td></tr></c:if>
      <c:forEach items="${vouchers}" var="r">
        <c:set var="vc" value="${r.voucher}"/>
        <tr>
          <td><strong><c:out value="${vc.code}"/></strong><c:if test="${r.autoIssued}"> <span class="badge badge-light border">tự động</span></c:if></td>
          <td><c:choose>
            <c:when test="${vc.discountType == 'PERCENT'}">${f:number(vc.discountValue)}%<c:if test="${not empty vc.maxDiscount}"><small class="text-muted"> tối đa ${f:vnd(vc.maxDiscount)}</small></c:if></c:when>
            <c:otherwise>${f:vnd(vc.discountValue)}</c:otherwise>
          </c:choose></td>
          <td>${vc.eligibleCustomer.label}</td>
          <td class="text-right">${f:vnd(vc.minOrderValue)}</td>
          <td class="text-center">${vc.quantity}</td>
          <td><small>${f:dateTime(vc.startDate)} – ${f:dateTime(vc.endDate)}</small></td>
          <td>${r.state}</td>
          <td class="text-right"><c:if test="${r.editable}"><a href="${ctx}/admin/vouchers/edit?code=${fn:escapeXml(vc.code)}">Sửa</a></c:if></td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div></div>
<p class="text-muted small mt-2">Voucher không bao giờ bị xóa. Voucher thủ công chỉ sửa được trước ngày bắt đầu; muốn đổi mã thì tạo voucher mới và đặt số lượng 0 cho voucher cũ (Q63).
  Voucher hằng tháng và ngày lễ chỉnh qua mục Voucher tự động.</p>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
