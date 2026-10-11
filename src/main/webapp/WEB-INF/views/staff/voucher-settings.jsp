<%-- Thiết lập voucher tự động (FR-37 hằng tháng theo hạng, FR-38 ngày lễ); lưu trong file JSON (mục 3.5), áp dụng từ lần phát hành sau (Q64). --%>
<c:set var="pageTitle" value="Voucher tự động"/>
<c:set var="staffNav" value="voucher-settings"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<h1 class="h3 text-black mb-2">Voucher tự động</h1>
<p class="text-muted small mb-4">Hệ thống tự phát hành, quản lý không phải bấm phát hành (Q48). Thay đổi chỉ áp dụng từ lần phát hành sau và giữ đến khi đổi tiếp;
  voucher đã phát hành không bị sửa (Q64). Bật một hạng hoặc chiến dịch giữa kỳ thì có voucher từ kỳ sau (Q69).</p>

<h2 class="h5 text-black">Voucher hằng tháng theo hạng thành viên</h2>
<p class="small text-muted">Mã &lt;HẠNG&gt;-&lt;MMYYYY&gt; (ví dụ LOYAL-102026), dành riêng cho hạng; phát hành lúc 00:00 ngày đầu tháng (server tắt thì phát hành bù).</p>
<div class="row">
  <c:forEach items="${settings.monthly}" var="e">
    <c:set var="k" value="${e.key}"/><c:set var="m" value="${e.value}"/><c:set var="mine" value="${formKey == k.name()}"/>
    <div class="col-xl-4 mb-4" id="${k}">
      <form action="${ctx}/admin/voucher-settings/monthly" method="post" class="card h-100" novalidate><div class="card-body">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="key" value="${k}">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <h3 class="h6 mb-0">Hạng ${k.label}</h3>
          <div class="custom-control custom-switch">
            <input type="checkbox" class="custom-control-input" id="en_${k}" name="enabled" ${(mine ? param.enabled == 'on' : m.enabled) ? 'checked' : ''}>
            <label class="custom-control-label" for="en_${k}">Bật</label>
          </div>
        </div>
        <c:if test="${m.enabled and not empty m.enabledAt}"><p class="small text-muted mb-2">Bật từ ${f:dateTime(m.enabledAt)}</p></c:if>
        <%@ include file="/WEB-INF/views/staff/voucher-setting-fields.jspf" %>
        <div class="row">
          <t:field name="startDay" label="Từ ngày" type="number" min="1" max="31" value="${mine ? param.startDay : m.startDay}"
                   error="${mine ? errors.startDay : ''}" cssClass="col-6"/>
          <t:field name="endDay" label="Đến ngày" type="number" min="1" max="31" value="${mine ? param.endDay : m.endDay}"
                   error="${mine ? errors.endDay : ''}" hint="Trống = cuối tháng" cssClass="col-6"/>
        </div>
        <button type="submit" class="btn btn-primary btn-sm">Lưu</button>
      </div></form>
    </div>
  </c:forEach>
</div>

<h2 class="h5 text-black">Voucher ngày lễ</h2>
<p class="small text-muted">Mã = tiền tố + số lần triển khai (2 chữ số, đếm cộng dồn qua các năm). Phải nhập loại và giá trị giảm trước khi bật (Q49).
  Thời gian hiệu lực tính từ 00:00 ngày lễ + số ngày lệch, kéo dài số ngày × 24 giờ.</p>
<div class="row">
  <c:forEach items="${settings.holidays}" var="e">
    <c:set var="k" value="${e.key}"/><c:set var="m" value="${e.value}"/><c:set var="mine" value="${formKey == k.name()}"/>
    <div class="col-xl-6 mb-4" id="${k}">
      <form action="${ctx}/admin/voucher-settings/holiday" method="post" class="card h-100" novalidate><div class="card-body">
        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
        <input type="hidden" name="key" value="${k}">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <h3 class="h6 mb-0">${k} – ${k.label}</h3>
          <div class="custom-control custom-switch">
            <input type="checkbox" class="custom-control-input" id="en_${k}" name="enabled" ${(mine ? param.enabled == 'on' : m.enabled) ? 'checked' : ''}>
            <label class="custom-control-label" for="en_${k}">Bật</label>
          </div>
        </div>
        <c:set var="occ" value="${occurrences[k]}"/>
        <p class="small text-muted mb-2">Lần tới: ${f:dateTime(occ[0])} – ${f:dateTime(occ[1])}
          <c:if test="${m.enabled and not empty m.enabledAt}"> · bật từ ${f:dateTime(m.enabledAt)}</c:if></p>
        <%@ include file="/WEB-INF/views/staff/voucher-setting-fields.jspf" %>
        <div class="row">
          <t:field name="eligibleCustomer" label="Đối tượng" cssClass="col-md-4">
            <select id="eligibleCustomer_${k}" name="eligibleCustomer" class="form-control form-control-sm">
              <c:forEach items="${eligibles}" var="el"><option value="${el}" ${(mine ? param.eligibleCustomer : m.eligibleCustomer) == el.name() ? 'selected' : ''}>${el.label}</option></c:forEach>
            </select>
          </t:field>
          <t:field name="startOffsetDays" label="Lệch (ngày)" type="number" value="${mine ? param.startOffsetDays : m.startOffsetDays}"
                   error="${mine ? errors.startOffsetDays : ''}" cssClass="col-md-4"/>
          <t:field name="durationDays" label="Số ngày hiệu lực" type="number" min="1" max="60" value="${mine ? param.durationDays : m.durationDays}"
                   error="${mine ? errors.durationDays : ''}" cssClass="col-md-4"/>
        </div>
        <button type="submit" class="btn btn-primary btn-sm">Lưu</button>
      </div></form>
    </div>
  </c:forEach>
</div>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
