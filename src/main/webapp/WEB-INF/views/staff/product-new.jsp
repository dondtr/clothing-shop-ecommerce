<%-- Thêm mặt hàng (FR-31 bước 1): thông tin chung và danh sách biến thể (màu, kích thước, tồn kho, ảnh). --%>
<c:set var="pageTitle" value="Thêm mặt hàng"/>
<c:set var="staffNav" value="products"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<p class="mb-2"><a href="${ctx}/staff/products">← Danh sách sản phẩm</a></p>
<h1 class="h3 text-black mb-3">Thêm mặt hàng</h1>
<c:if test="${not empty errors}">
  <div class="alert alert-danger">Vui lòng kiểm tra lại:
    <ul class="mb-0"><c:forEach items="${errors}" var="e"><li><c:out value="${e.value}"/></li></c:forEach></ul>
  </div>
</c:if>
<form action="${ctx}/staff/products/create" method="post" novalidate>
  <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
  <div class="card mb-3"><div class="card-body">
    <h2 class="h6 text-uppercase text-muted">Thông tin chung</h2>
    <div class="row">
      <t:field name="name" label="Tên mặt hàng" value="${param.name}" required="true" maxlength="200" error="${errors.name}"
               hint="Không trùng tên mặt hàng khác (BR-28)" cssClass="col-md-6"/>
      <t:field name="category" label="Danh mục" required="true" error="${errors.category}" cssClass="col-md-3">
        <select id="category" name="category" class="form-control">
          <c:forEach items="${categories}" var="c"><option value="${c}" ${param.category == c ? 'selected' : ''}>${c.label}</option></c:forEach>
        </select>
      </t:field>
      <t:field name="basePrice" label="Giá bán (₫)" type="number" value="${param.basePrice}" required="true" min="1000" step="1000"
               error="${errors.basePrice}" hint="Bội số của 1.000" cssClass="col-md-3"/>
    </div>
    <t:field name="description" label="Mô tả" error="${errors.description}">
      <textarea id="description" name="description" class="form-control" rows="3" maxlength="5000"><c:out value="${param.description}"/></textarea>
    </t:field>
    <div class="form-check">
      <input class="form-check-input" type="checkbox" id="active" name="active" ${empty param.name or param.active == 'on' ? 'checked' : ''}>
      <label class="form-check-label" for="active">Đang bán (hiện ở cửa hàng)</label>
    </div>
  </div></div>

  <div class="card mb-3"><div class="card-body">
    <h2 class="h6 text-uppercase text-muted">Biến thể</h2>
    <p class="small text-muted">Mỗi dòng là một màu + kích thước với tồn kho và ảnh riêng; hệ thống lưu mỗi biến thể thành một dòng Product mang đủ thông tin chung (Q01).
      Ảnh nhập đường dẫn trong ứng dụng (ví dụ assets/images/cloth_1.jpg) hoặc địa chỉ http(s).</p>
    <c:if test="${not empty errors.variants}"><div class="text-danger small mb-2"><c:out value="${errors.variants}"/></div></c:if>
    <table class="table table-sm" id="variants">
      <thead><tr><th>Màu</th><th>Kích thước</th><th style="width: 120px;">Tồn kho</th><th>Ảnh</th><th></th></tr></thead>
      <tbody>
        <c:set var="colors" value="${paramValues.color}"/>
        <c:forEach begin="0" end="${empty colors ? 1 : fn:length(colors) - 1}" var="i">
          <tr>
            <td><input type="text" name="color" class="form-control form-control-sm" maxlength="50" value="${fn:escapeXml(paramValues.color[i])}" placeholder="Ví dụ: Trắng"></td>
            <td><input type="text" name="size" class="form-control form-control-sm" maxlength="20" value="${fn:escapeXml(paramValues.size[i])}" placeholder="Ví dụ: M"></td>
            <td><input type="number" name="stockQuantity" class="form-control form-control-sm" min="0" value="${empty paramValues.stockQuantity[i] ? '0' : fn:escapeXml(paramValues.stockQuantity[i])}"></td>
            <td><input type="text" name="imgUrl" class="form-control form-control-sm" maxlength="255" list="image-list" value="${fn:escapeXml(paramValues.imgUrl[i])}"></td>
            <td><button type="button" class="btn btn-sm btn-link text-danger js-remove-row" aria-label="Bỏ dòng">✕</button></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
    <button type="button" class="btn btn-sm btn-outline-primary" id="add-row">+ Thêm biến thể</button>
    <datalist id="image-list"><c:forEach items="${images}" var="img"><option value="${img}"></c:forEach></datalist>
  </div></div>
  <button type="submit" class="btn btn-primary">Lưu mặt hàng</button>
</form>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
<script>
  $('#add-row').on('click', function () {
    var row = $('#variants tbody tr:last').clone();
    row.find('input').val('');
    row.find('input[name=stockQuantity]').val('0');
    $('#variants tbody').append(row);
  });
  $('#variants').on('click', '.js-remove-row', function () {
    if ($('#variants tbody tr').length > 1) { $(this).closest('tr').remove(); }
  });
</script>
</body>
</html>
