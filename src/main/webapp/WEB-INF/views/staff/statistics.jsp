<%-- Thống kê kinh doanh (FR-43), chỉ quản lý: doanh thu, đơn hàng, số lượng bán ra, tồn kho. --%>
<c:set var="pageTitle" value="Thống kê"/>
<c:set var="staffNav" value="statistics"/>
<%@ include file="/WEB-INF/views/staff/top.jspf" %>
<c:set var="r" value="${report}"/>
<c:set var="n" value="${fn:length(r.series)}"/>
<c:set var="unit" value="${r.monthly ? 'tháng' : 'ngày'}"/>

<div class="d-flex justify-content-between align-items-center flex-wrap mb-3">
  <h1 class="h3 text-black mb-0">Thống kê kinh doanh</h1>
  <span class="text-muted">Từ ${f:date(r.from)} đến ${f:date(r.to)}</span>
</div>
<form action="${ctx}/admin/statistics" method="get" class="form-inline flex-wrap mb-2">
  <label for="from" class="mr-2">Từ ngày</label>
  <input type="date" id="from" name="from" value="${r.from}" class="form-control form-control-sm mr-3" required>
  <label for="to" class="mr-2">Đến ngày</label>
  <input type="date" id="to" name="to" value="${r.to}" class="form-control form-control-sm mr-3" required>
  <button type="submit" class="btn btn-sm btn-primary">Xem</button>
</form>
<div class="mb-4 small">
  <c:forEach items="${presets}" var="p" varStatus="s">
    <a href="${ctx}/admin/statistics?from=${p.from}&amp;to=${p.to}" class="${p.from == r.from && p.to == r.to ? 'font-weight-bold text-black' : ''}">${p.label}</a><c:if test="${not s.last}"> · </c:if>
  </c:forEach>
</div>
<c:if test="${not empty errors}">
  <div class="alert alert-warning"><c:out value="${errors.to}"/>. Đang hiển thị tháng này.</div>
</c:if>

<div class="row">
  <div class="col-md-6 col-xl-3 mb-4"><div class="card stat-card h-100"><div class="card-body">
    <div class="label">Doanh thu</div><div class="value">${f:vnd(r.revenue)}</div>
    <div class="small text-muted">Đơn đã thanh toán, gồm phí vận chuyển</div>
  </div></div></div>
  <div class="col-md-6 col-xl-3 mb-4"><div class="card stat-card h-100"><div class="card-body">
    <div class="label">Đơn đã thanh toán</div><div class="value">${r.paidOrders}</div>
    <div class="small text-muted">Theo ngày thanh toán</div>
  </div></div></div>
  <div class="col-md-6 col-xl-3 mb-4"><div class="card stat-card h-100"><div class="card-body">
    <div class="label">Đơn đặt trong kỳ</div><div class="value">${r.placedOrders}</div>
    <div class="small text-muted">Theo ngày đặt, mọi trạng thái</div>
  </div></div></div>
  <div class="col-md-6 col-xl-3 mb-4"><div class="card stat-card h-100"><div class="card-body">
    <div class="label">Sản phẩm bán ra</div><div class="value">${r.soldQuantity}</div>
    <div class="small text-muted">Tổng số lượng trong đơn đã thanh toán</div>
  </div></div></div>
</div>

<div class="card mb-4">
  <div class="card-body">
    <h2 class="h6 text-uppercase text-muted mb-1">Doanh thu theo ${unit}</h2>
    <p class="small text-muted mb-4">Đơn đã thanh toán, gồm phí vận chuyển. Rê chuột (hoặc Tab) vào cột để xem số liệu.</p>
    <div class="rev-chart" role="group" aria-label="Biểu đồ doanh thu theo ${unit}, từ ${f:date(r.from)} đến ${f:date(r.to)}">
      <div class="rev-axis" aria-hidden="true">
        <c:forEach items="${r.ticks}" var="tick"><span style="bottom: ${tick.percent}%">${f:vndShort(tick.amount)}</span></c:forEach>
      </div>
      <div class="rev-plot">
        <c:forEach items="${r.ticks}" var="tick" varStatus="s"><div class="rev-grid ${s.first ? 'base' : ''}" style="bottom: ${tick.percent}%"></div></c:forEach>
        <div class="rev-cols">
          <c:forEach items="${r.series}" var="b" varStatus="s">
            <div class="rev-col ${s.index * 4 < n ? 'tip-left' : (s.index * 4 >= n * 3 ? 'tip-right' : '')}" style="--h: ${b.heightPercent}%"
                 tabindex="0" aria-label="${b.label}: ${f:vnd(b.revenue)}, ${b.paidOrders} đơn thanh toán, ${b.placedOrders} đơn đặt">
              <div class="rev-bar"></div>
              <div class="rev-tip" aria-hidden="true"><strong>${b.label}</strong><br>Doanh thu: ${f:vnd(b.revenue)}<br>
                Đơn thanh toán: ${b.paidOrders} · Đơn đặt: ${b.placedOrders}</div>
              <c:if test="${s.index % r.labelEvery == 0}"><span class="rev-x" aria-hidden="true">${b.label}</span></c:if>
            </div>
          </c:forEach>
        </div>
      </div>
    </div>
    <details>
      <summary class="small">Xem dạng bảng</summary>
      <table class="table table-sm mt-2 mb-0">
        <thead class="thead-light"><tr><th>${r.monthly ? 'Tháng' : 'Ngày'}</th><th class="text-right">Doanh thu</th>
          <th class="text-right">Đơn thanh toán</th><th class="text-right">Đơn đặt</th></tr></thead>
        <tbody>
          <c:forEach items="${r.series}" var="b">
            <tr><td>${b.label}</td><td class="text-right">${f:vnd(b.revenue)}</td><td class="text-right">${b.paidOrders}</td>
              <td class="text-right">${b.placedOrders}</td></tr>
          </c:forEach>
        </tbody>
      </table>
    </details>
  </div>
</div>

<div class="row">
  <div class="col-lg-5 mb-4">
    <div class="card h-100">
      <div class="card-body">
        <h2 class="h6 text-uppercase text-muted mb-3">Đơn đặt theo trạng thái</h2>
        <table class="table table-sm mb-0">
          <tbody>
            <c:forEach items="${r.ordersByStatus}" var="e">
              <tr><td><t:status value="${e.key}"/></td><td class="text-right">${e.value}</td></tr>
            </c:forEach>
            <tr class="font-weight-bold"><td>Tổng</td><td class="text-right">${r.placedOrders}</td></tr>
          </tbody>
        </table>
        <p class="small text-muted mt-3 mb-0">Đơn đặt từ ${f:date(r.from)} đến ${f:date(r.to)}, trạng thái hiện tại.
          Không tính đơn đang chờ thanh toán VNPAY (BR-26).</p>
      </div>
    </div>
  </div>
  <div class="col-lg-7 mb-4">
    <div class="card h-100">
      <div class="card-body">
        <h2 class="h6 text-uppercase text-muted mb-3">Bán ra theo mặt hàng</h2>
        <table class="table table-sm mb-0">
          <thead class="thead-light"><tr><th>Mặt hàng</th><th class="text-right">Số lượng</th><th class="text-right">Doanh thu</th></tr></thead>
          <tbody>
            <c:if test="${empty r.products}"><tr><td colspan="3" class="text-center text-muted py-3">Chưa có đơn thanh toán trong kỳ.</td></tr></c:if>
            <c:forEach items="${r.products}" var="p">
              <tr><td><c:out value="${p.name}"/></td><td class="text-right">${p.quantity}</td><td class="text-right">${f:vnd(p.revenue)}</td></tr>
            </c:forEach>
          </tbody>
        </table>
        <p class="small text-muted mt-3 mb-0">Doanh thu mặt hàng = tổng thành tiền các dòng của đơn đã thanh toán, trước khi trừ voucher
          và không gồm phí vận chuyển, nên tổng có thể khác doanh thu ở trên.</p>
      </div>
    </div>
  </div>
</div>

<div class="card mb-4">
  <div class="card-body">
    <h2 class="h6 text-uppercase text-muted mb-3">Bán ra theo dòng (màu, kích thước)</h2>
    <table class="table table-sm mb-0">
      <thead class="thead-light"><tr><th>Mặt hàng</th><th>Màu</th><th>Kích thước</th><th class="text-right">Số lượng</th>
        <th class="text-right">Doanh thu</th></tr></thead>
      <tbody>
        <c:if test="${empty r.rows}"><tr><td colspan="5" class="text-center text-muted py-3">Chưa có đơn thanh toán trong kỳ.</td></tr></c:if>
        <c:forEach items="${r.rows}" var="row">
          <tr><td><c:out value="${row.name}"/></td><td><c:out value="${row.color}"/></td><td><c:out value="${row.size}"/></td>
            <td class="text-right">${row.quantity}</td><td class="text-right">${f:vnd(row.revenue)}</td></tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>

<div class="card mb-4">
  <div class="card-body">
    <h2 class="h6 text-uppercase text-muted mb-3">Tồn kho hiện tại</h2>
    <table class="table table-sm mb-0">
      <thead class="thead-light"><tr><th>Mặt hàng</th><th>Tình trạng</th><th class="text-right">Tổng tồn</th><th>Theo dòng (màu / kích thước: tồn)</th></tr></thead>
      <tbody>
        <c:forEach items="${r.stock}" var="st">
          <tr>
            <td><a href="${ctx}/staff/products/edit?id=${st.firstId}"><c:out value="${st.name}"/></a></td>
            <td>${st.active ? 'Đang bán' : '<span class="text-muted">Ngừng bán</span>'}</td>
            <td class="text-right ${st.total == 0 ? 'text-danger font-weight-bold' : ''}">${st.total}</td>
            <td class="small">
              <c:forEach items="${st.rows}" var="sr" varStatus="s">
                <span class="${sr.stock == 0 ? 'text-danger' : ''}"><c:out value="${sr.color}"/> / <c:out value="${sr.size}"/>: ${sr.stock}</span><c:if test="${not s.last}"> · </c:if>
              </c:forEach>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>
</div>

<%@ include file="/WEB-INF/views/staff/bottom.jspf" %>
</body>
</html>
