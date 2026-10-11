<%-- Sửa mặt hàng (FR-31 – FR-33): thông tin chung, trạng thái kinh doanh, biến thể, tồn kho; quản lý xóa và đặt giảm giá. --%>
<c:set var="p" value="${product}"/>
<c:set var="pageTitle" value="${p.name}"/>
<c:set var="staffNav" value="products"/>
<c:set var="editingCommon" value="${formTarget == '/staff/products/update'}"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<p class="mb-2"><a href="${ctx}/staff/products">← Danh sách sản phẩm</a></p>
<div class="d-flex justify-content-between align-items-center flex-wrap mb-3">
  <h1 class="h3 text-black mb-0"><c:out value="${p.name}"/>
    <span class="badge ${p.active ? 'badge-success' : 'badge-secondary'} align-middle">${p.active ? 'Đang bán' : 'Không bán'}</span></h1>
  <div class="d-flex flex-wrap" style="gap: 8px;">
    <c:if test="${p.active}"><a href="${ctx}/product?id=${p.firstId}" target="_blank" rel="noopener" class="btn btn-sm btn-light border">Xem ở cửa hàng ↗</a></c:if>
    <form action="${ctx}/staff/products/active" method="post" class="m-0"
          data-confirm="${p.active ? 'Chuyển mặt hàng sang không bán? Khách sẽ không thấy ở cửa hàng; dòng trong giỏ không đặt được.' : 'Chuyển mặt hàng sang đang bán?'}">
      <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
      <input type="hidden" name="id" value="${p.firstId}">
      <input type="hidden" name="stamp" value="${p.stamp}">
      <input type="hidden" name="active" value="${not p.active}">
      <button type="submit" class="btn btn-sm ${p.active ? 'btn-outline-secondary' : 'btn-outline-success'}">${p.active ? 'Ngừng bán' : 'Mở bán'}</button>
    </form>
    <c:if test="${sessionScope.auth.admin}">
      <a href="${ctx}/admin/discounts?id=${p.firstId}" class="btn btn-sm btn-outline-primary">Giảm giá sản phẩm</a>
      <c:if test="${p.canDelete}">
        <form action="${ctx}/admin/products/delete" method="post" class="m-0" data-confirm="Xóa hẳn mặt hàng và mọi biến thể? Không hoàn tác được.">
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <input type="hidden" name="id" value="${p.firstId}">
          <button type="submit" class="btn btn-sm btn-outline-danger">Xóa mặt hàng</button>
        </form>
      </c:if>
    </c:if>
  </div>
</div>

<div class="card mb-4"><div class="card-body">
  <h2 class="h6 text-uppercase text-muted">Thông tin chung (áp dụng cho mọi biến thể)</h2>
  <c:if test="${editingCommon and not empty errors}"><div class="alert alert-danger py-2">Vui lòng kiểm tra lại các ô được đánh dấu.</div></c:if>
  <form action="${ctx}/staff/products/update" method="post" novalidate>
    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
    <input type="hidden" name="id" value="${p.firstId}">
    <input type="hidden" name="stamp" value="${p.stamp}">
    <div class="row">
      <t:field name="name" label="Tên mặt hàng" value="${editingCommon ? param.name : p.name}" required="true" maxlength="200"
               error="${editingCommon ? errors.name : ''}" hint="Đổi tên thì đơn cũ hiển thị tên mới" cssClass="col-md-6"/>
      <t:field name="category" label="Danh mục" required="true" cssClass="col-md-3">
        <select id="category" name="category" class="form-control">
          <c:forEach items="${categories}" var="c"><option value="${c}" ${(editingCommon ? param.category : p.category) == c.name() ? 'selected' : ''}>${c.label}</option></c:forEach>
        </select>
      </t:field>
      <t:field name="basePrice" label="Giá bán (₫)" type="number" value="${editingCommon ? param.basePrice : f:number(p.basePrice)}" required="true"
               min="1000" step="1000" error="${editingCommon ? errors.basePrice : ''}" hint="Đơn cũ không đổi (đã lưu thành tiền)" cssClass="col-md-3"/>
    </div>
    <t:field name="description" label="Mô tả" error="${editingCommon ? errors.description : ''}">
      <textarea id="description" name="description" class="form-control" rows="3" maxlength="5000"><c:out value="${editingCommon ? param.description : p.description}"/></textarea>
    </t:field>
    <input type="hidden" name="active" value="${p.active}">
    <button type="submit" class="btn btn-primary btn-sm">Lưu thông tin chung</button>
  </form>
</div></div>

<div class="card mb-4"><div class="card-body">
  <h2 class="h6 text-uppercase text-muted">Biến thể và tồn kho</h2>
  <p class="small text-muted">Biến thể đã có trong đơn hàng không đổi được màu, kích thước và không xóa được (BR-15); muốn ngừng thì chuyển mặt hàng sang không bán hoặc đặt tồn kho về 0.</p>
  <c:if test="${not empty errors and not editingCommon}">
    <div class="alert alert-danger py-2"><c:forEach items="${errors}" var="e"><div><c:out value="${e.value}"/></div></c:forEach></div>
  </c:if>
  <div class="table-responsive">
    <table class="table table-sm">
      <thead><tr><th>Ảnh</th><th>Màu / kích thước / ảnh</th><th>Tồn kho</th><th></th></tr></thead>
      <tbody>
        <c:forEach items="${p.rows}" var="r">
          <tr>
            <td style="width: 60px;"><img src="${fn:escapeXml(f:imageUrl(ctx, r.imgUrl))}" alt="" style="width: 48px; height: 48px; object-fit: cover;" class="rounded"></td>
            <td>
              <form action="${ctx}/staff/products/variant/update" method="post" class="form-inline flex-nowrap m-0" style="gap: 6px;">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="id" value="${r.id}">
                <input type="hidden" name="version" value="${r.version}">
                <input type="text" name="color" class="form-control form-control-sm" style="width: 110px;" maxlength="50" value="${fn:escapeXml(r.color)}" ${r.sold ? 'readonly' : ''} aria-label="Màu">
                <input type="text" name="size" class="form-control form-control-sm" style="width: 80px;" maxlength="20" value="${fn:escapeXml(r.size)}" ${r.sold ? 'readonly' : ''} aria-label="Kích thước">
                <input type="text" name="imgUrl" class="form-control form-control-sm" style="width: 220px;" maxlength="255" list="image-list" value="${fn:escapeXml(r.imgUrl)}" aria-label="Ảnh">
                <button type="submit" class="btn btn-sm btn-outline-primary">Lưu</button>
                <c:if test="${r.sold}"><small class="text-muted">đã bán</small></c:if>
              </form>
            </td>
            <td>
              <form action="${ctx}/staff/products/stock" method="post" class="form-inline flex-nowrap m-0" style="gap: 6px;">
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="id" value="${r.id}">
                <input type="hidden" name="version" value="${r.version}">
                <strong class="${r.stockQuantity == 0 ? 'text-danger' : ''}" style="min-width: 36px;">${r.stockQuantity}</strong>
                <select name="mode" class="form-control form-control-sm" aria-label="Cách cập nhật">
                  <option value="add">Cộng thêm</option>
                  <option value="set">Đặt thành</option>
                </select>
                <input type="number" name="quantity" class="form-control form-control-sm" style="width: 90px;" required aria-label="Số lượng">
                <button type="submit" class="btn btn-sm btn-outline-primary">Cập nhật</button>
              </form>
            </td>
            <td class="text-right">
              <c:if test="${sessionScope.auth.admin and not r.sold}">
                <form action="${ctx}/admin/products/variant/delete" method="post" class="m-0" data-confirm="Xóa biến thể ${fn:escapeXml(r.color)} / ${fn:escapeXml(r.size)}?">
                  <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                  <input type="hidden" name="id" value="${r.id}">
                  <c:forEach items="${p.rows}" var="other"><c:if test="${empty back and other.id != r.id}"><c:set var="back" value="${other.id}"/></c:if></c:forEach>
                  <input type="hidden" name="back" value="${back}">
                  <c:remove var="back"/>
                  <button type="submit" class="btn btn-sm btn-link text-danger">Xóa</button>
                </form>
              </c:if>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
  <h3 class="h6 mt-3">Thêm biến thể</h3>
  <form action="${ctx}/staff/products/variant/add" method="post" class="form-inline flex-wrap" style="gap: 6px;" novalidate>
    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
    <input type="hidden" name="id" value="${p.firstId}">
    <input type="hidden" name="stamp" value="${p.stamp}">
    <input type="text" name="color" class="form-control form-control-sm" maxlength="50" placeholder="Màu" required>
    <input type="text" name="size" class="form-control form-control-sm" maxlength="20" placeholder="Kích thước" required>
    <input type="number" name="stockQuantity" class="form-control form-control-sm" min="0" value="0" style="width: 90px;" aria-label="Tồn kho">
    <input type="text" name="imgUrl" class="form-control form-control-sm" maxlength="255" list="image-list" placeholder="Ảnh" style="width: 220px;" required>
    <button type="submit" class="btn btn-sm btn-primary">Thêm</button>
  </form>
  <p class="small text-muted mt-2 mb-0">Biến thể mới nhận bản sao các đợt giảm giá chưa kết thúc để mọi biến thể có cùng giá sau giảm.</p>
  <datalist id="image-list"><c:forEach items="${images}" var="img"><option value="${img}"></c:forEach></datalist>
</div></div>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
