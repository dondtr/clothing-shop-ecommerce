<%-- Xóa địa chỉ mặc định (FR-06 bước 5, Q62, SD13): phải chọn địa chỉ khác hoặc tạo địa chỉ mới làm mặc định. --%>
<c:set var="pageTitle" value="Xóa địa chỉ mặc định"/>
<c:set var="accountNav" value="addresses"/>
<c:set var="creating" value="${empty others or param.choice == 'new'}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span> <a href="${ctx}/account/addresses">Sổ địa chỉ</a>
    <span class="mx-2 mb-0">/</span> <strong class="text-black">Xóa địa chỉ mặc định</strong>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row">
      <div class="col-md-3"><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></div>
      <div class="col-md-9">
        <h2 class="h4 text-black mb-3">Xóa địa chỉ mặc định</h2>
        <div class="alert alert-warning">
          Bạn đang xóa địa chỉ mặc định: <strong><c:out value="${target.text}"/></strong>.<br>
          Hãy chọn địa chỉ mặc định mới<c:if test="${not empty others}"> trong các địa chỉ còn lại hoặc tạo địa chỉ mới</c:if><c:if test="${empty others}"> bằng cách tạo địa chỉ mới (bạn không còn địa chỉ nào khác)</c:if>.
          Địa chỉ cũ chỉ bị xóa khi bạn xác nhận.
        </div>
        <form action="${ctx}/account/addresses/remove-default" method="post" class="border rounded p-3" novalidate>
          <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
          <input type="hidden" name="id" value="${target.id}">
          <c:forEach items="${others}" var="a" varStatus="st">
            <div class="form-check mb-2">
              <input class="form-check-input" type="radio" name="choice" id="existing_${a.id}" value="existing"
                     data-address="${a.id}" ${not creating and st.first ? 'checked' : ''}>
              <label class="form-check-label" for="existing_${a.id}"><c:out value="${a.text}"/></label>
            </div>
          </c:forEach>
          <input type="hidden" name="newDefaultId" id="newDefaultId" value="${empty others ? '' : others[0].id}">
          <div class="form-check mb-2">
            <input class="form-check-input" type="radio" name="choice" id="choice_new" value="new" ${creating ? 'checked' : ''}>
            <label class="form-check-label" for="choice_new">Tạo địa chỉ mới làm mặc định</label>
          </div>
          <div id="new-address" class="row pl-4 ${creating ? '' : 'd-none'}">
            <t:field name="street" label="Số nhà, tên đường" value="${form.street}" required="true" maxlength="255" error="${errors.street}" cssClass="col-md-12"/>
            <t:field name="ward" label="Phường/xã" value="${form.ward}" required="true" maxlength="100" error="${errors.ward}" cssClass="col-md-6"/>
            <t:field name="province" label="Tỉnh/thành phố" value="${form.province}" required="true" maxlength="100" error="${errors.province}" cssClass="col-md-6"/>
          </div>
          <div class="d-flex mt-3" style="gap: 8px;">
            <button type="submit" class="btn btn-danger btn-sm">Xóa và đổi địa chỉ mặc định</button>
            <a href="${ctx}/account/addresses" class="btn btn-outline-secondary btn-sm">Hủy, giữ địa chỉ</a>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
<script>
  $('input[name=choice]').on('change', function () {
    var isNew = $('#choice_new').is(':checked');
    $('#new-address').toggleClass('d-none', !isNew);
    if (!isNew) { $('#newDefaultId').val($(this).attr('data-address')); }
  });
</script>
</body>
</html>
