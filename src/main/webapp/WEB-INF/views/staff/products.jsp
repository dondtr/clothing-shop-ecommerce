<%-- Sản phẩm (FR-31 – FR-33): danh sách mặt hàng, kể cả đang không bán. --%>
<c:set var="pageTitle" value="Sản phẩm"/>
<c:set var="staffNav" value="products"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>

<div class="d-flex justify-content-between align-items-center flex-wrap mb-3">
  <h1 class="h3 text-black mb-0">Sản phẩm, tồn kho</h1>
  <a href="${ctx}/staff/products/new" class="btn btn-primary btn-sm">+ Thêm mặt hàng</a>
</div>
<form action="${ctx}/staff/products" method="get" class="form-inline mb-3">
  <input type="text" name="q" value="${fn:escapeXml(param.q)}" class="form-control form-control-sm mr-2" style="min-width: 260px;"
         placeholder="Tìm theo tên mặt hàng" maxlength="200">
  <button type="submit" class="btn btn-sm btn-outline-primary">Tìm</button>
</form>
<div class="card"><div class="card-body p-0">
  <table class="table table-sm table-hover mb-0">
    <thead class="thead-light"><tr><th></th><th>Mặt hàng</th><th>Danh mục</th><th class="text-right">Giá bán</th><th class="text-center">Biến thể</th><th class="text-right">Tồn kho</th><th>Trạng thái</th></tr></thead>
    <tbody>
      <c:if test="${empty products}"><tr><td colspan="7" class="text-center text-muted py-3">Chưa có mặt hàng nào.</td></tr></c:if>
      <c:forEach items="${products}" var="p">
        <tr>
          <td style="width: 52px;"><img src="${fn:escapeXml(f:imageUrl(ctx, p.imgUrl))}" alt="" style="width: 40px; height: 40px; object-fit: cover;" class="rounded"></td>
          <td><a href="${ctx}/staff/products/edit?id=${p.firstId}"><c:out value="${p.name}"/></a></td>
          <td>${p.category.label}</td>
          <td class="text-right">${f:vnd(p.basePrice)}</td>
          <td class="text-center">${p.variants}</td>
          <td class="text-right ${p.stock == 0 ? 'text-danger' : ''}">${p.stock}</td>
          <td><span class="badge ${p.active ? 'badge-success' : 'badge-secondary'}">${p.active ? 'Đang bán' : 'Không bán'}</span></td>
        </tr>
      </c:forEach>
    </tbody>
  </table>
</div></div>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
