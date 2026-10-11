<%-- Sổ địa chỉ (FR-06): thêm, sửa, đặt mặc định, xóa. Người nhận luôn là chủ tài khoản (Q06). --%>
<c:set var="pageTitle" value="Sổ địa chỉ"/>
<c:set var="accountNav" value="addresses"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <strong class="text-black">Sổ địa chỉ</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row">
      <div class="col-md-3"><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></div>
      <div class="col-md-9">
        <h2 class="h4 text-black mb-1">Sổ địa chỉ</h2>
        <p class="text-muted small">Người nhận hàng là chủ tài khoản: <c:out value="${sessionScope.auth.fullName}"/>.
          Sửa hoặc xóa địa chỉ không ảnh hưởng các đơn đã đặt.</p>

        <c:if test="${empty addresses}">
          <div class="alert alert-info">Bạn chưa có địa chỉ nào. Địa chỉ đầu tiên sẽ là địa chỉ mặc định.</div>
        </c:if>
        <c:forEach items="${addresses}" var="a">
          <c:set var="editing" value="${formTarget == '/account/addresses/update' and formId == a.id}"/>
          <div class="border rounded p-3 mb-3">
            <div class="d-flex justify-content-between align-items-start flex-wrap">
              <div>
                <strong class="text-black"><c:out value="${a.text}"/></strong>
                <c:if test="${a.defaultAddress}"><span class="badge badge-primary ml-2">Mặc định</span></c:if>
              </div>
              <div class="d-flex" style="gap: 8px;">
                <c:if test="${not a.defaultAddress}">
                  <form action="${ctx}/account/addresses/default" method="post" class="m-0">
                    <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="id" value="${a.id}">
                    <button type="submit" class="btn btn-sm btn-outline-primary">Đặt mặc định</button>
                  </form>
                </c:if>
                <form action="${ctx}/account/addresses/remove" method="post" class="m-0"
                      onsubmit="return ${a.defaultAddress} || confirm('Xóa địa chỉ này?');">
                  <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                  <input type="hidden" name="id" value="${a.id}">
                  <button type="submit" class="btn btn-sm btn-outline-danger">Xóa</button>
                </form>
              </div>
            </div>
            <details class="mt-2" ${editing ? 'open' : ''}>
              <summary class="small text-primary">Sửa địa chỉ</summary>
              <c:set var="f" value="${editing ? form : a}"/>
              <form action="${ctx}/account/addresses/update" method="post" class="mt-2" novalidate>
                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                <input type="hidden" name="id" value="${a.id}">
                <input type="hidden" name="version" value="${a.version}">
                <div class="row">
                  <t:field name="street" label="Số nhà, tên đường" value="${f.street}" required="true" maxlength="255"
                           error="${editing ? errors.street : ''}" cssClass="col-md-12"/>
                  <t:field name="ward" label="Phường/xã" value="${f.ward}" required="true" maxlength="100"
                           error="${editing ? errors.ward : ''}" cssClass="col-md-6"/>
                  <t:field name="province" label="Tỉnh/thành phố" value="${f.province}" required="true" maxlength="100"
                           error="${editing ? errors.province : ''}" cssClass="col-md-6"/>
                </div>
                <button type="submit" class="btn btn-sm btn-primary">Lưu</button>
              </form>
            </details>
          </div>
        </c:forEach>

        <c:set var="adding" value="${formTarget == '/account/addresses/add'}"/>
        <h3 class="h5 text-black mt-4">Thêm địa chỉ</h3>
        <form action="${ctx}/account/addresses/add" method="post" class="border rounded p-3" novalidate>
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <div class="row">
            <t:field name="street" label="Số nhà, tên đường" value="${adding ? form.street : ''}" required="true" maxlength="255"
                     error="${adding ? errors.street : ''}" cssClass="col-md-12" placeholder="Ví dụ: 1 Võ Văn Ngân"/>
            <t:field name="ward" label="Phường/xã" value="${adding ? form.ward : ''}" required="true" maxlength="100"
                     error="${adding ? errors.ward : ''}" cssClass="col-md-6"/>
            <t:field name="province" label="Tỉnh/thành phố" value="${adding ? form.province : ''}" required="true" maxlength="100"
                     error="${adding ? errors.province : ''}" cssClass="col-md-6"/>
          </div>
          <button type="submit" class="btn btn-primary btn-sm">Thêm địa chỉ</button>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
