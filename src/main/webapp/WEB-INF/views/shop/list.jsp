<%-- Cửa hàng: duyệt theo danh mục, tìm theo tên, khoảng giá (sau giảm) và màu (FR-07). --%>
<c:set var="pageTitle" value="${empty filter.category ? 'Cửa hàng' : filter.category.label}"/>
<c:set var="activeNav" value="shop"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="bg-light py-3">
  <div class="container">
    <div class="row">
      <div class="col-md-12 mb-0">
        <a href="${ctx}/">Trang chủ</a> <span class="mx-2 mb-0">/</span>
        <c:choose>
          <c:when test="${empty filter.category}"><strong class="text-black">Cửa hàng</strong></c:when>
          <c:otherwise>
            <a href="${ctx}/shop">Cửa hàng</a> <span class="mx-2 mb-0">/</span>
            <strong class="text-black"><c:out value="${filter.category.label}"/></strong>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</div>

<div class="site-section">
  <div class="container">
    <div class="row mb-5">
      <div class="col-md-9 order-2">
        <div class="mb-4">
          <h2 class="text-black h5">
            <c:choose>
              <c:when test="${not empty filter.query}">Kết quả tìm "<c:out value="${filter.query}"/>"</c:when>
              <c:when test="${not empty filter.category}"><c:out value="${filter.category.label}"/></c:when>
              <c:otherwise>Tất cả sản phẩm</c:otherwise>
            </c:choose>
            <small class="text-muted ml-1">(${result.totalItems} sản phẩm)</small>
          </h2>
        </div>
        <c:if test="${not empty error}">
          <div class="alert alert-warning"><c:out value="${error}"/></div>
        </c:if>

        <div class="row mb-5">
          <c:if test="${empty result.items}">
            <div class="col-12"><div class="alert alert-info">Không tìm thấy sản phẩm phù hợp. Hãy thử bỏ bớt bộ lọc.</div></div>
          </c:if>
          <c:forEach items="${result.items}" var="p">
            <div class="col-sm-6 col-lg-4 mb-4" data-aos="fade-up">
              <t:product-card product="${p}"/>
            </div>
          </c:forEach>
        </div>

        <c:if test="${result.totalPages > 1}">
          <c:set var="pageLink" value="${ctx}/shop?${empty pageQuery ? '' : pageQuery.concat('&')}page="/>
          <div class="row">
            <div class="col-md-12 text-center">
              <div class="site-block-27">
                <ul>
                  <c:if test="${result.page > 1}">
                    <li><a href="${fn:escapeXml(pageLink)}${result.page - 1}" aria-label="Trang trước">&lt;</a></li>
                  </c:if>
                  <c:forEach begin="1" end="${result.totalPages}" var="i">
                    <c:choose>
                      <c:when test="${i == result.page}"><li class="active"><span>${i}</span></li></c:when>
                      <c:otherwise><li><a href="${fn:escapeXml(pageLink)}${i}">${i}</a></li></c:otherwise>
                    </c:choose>
                  </c:forEach>
                  <c:if test="${result.page < result.totalPages}">
                    <li><a href="${fn:escapeXml(pageLink)}${result.page + 1}" aria-label="Trang sau">&gt;</a></li>
                  </c:if>
                </ul>
              </div>
            </div>
          </div>
        </c:if>
      </div>

      <div class="col-md-3 order-1 mb-5 mb-md-0">
        <div class="border p-4 rounded mb-4">
          <h3 class="mb-3 h6 text-uppercase text-black d-block">Danh mục</h3>
          <ul class="list-unstyled mb-0">
            <li class="mb-1"><a href="${ctx}/shop" class="${empty filter.category ? 'font-weight-bold' : ''}">Tất cả</a></li>
            <c:forEach items="${categories}" var="cat">
              <li class="mb-1">
                <a href="${ctx}/shop?category=${cat}" class="${filter.category == cat ? 'font-weight-bold' : ''}"><c:out value="${cat.label}"/></a>
              </li>
            </c:forEach>
          </ul>
        </div>

        <form action="${ctx}/shop" method="get" class="border p-4 rounded mb-4">
          <c:if test="${not empty filter.category}"><input type="hidden" name="category" value="${filter.category}"></c:if>
          <div class="form-group">
            <label for="q" class="h6 text-uppercase text-black d-block">Tên sản phẩm</label>
            <input type="text" id="q" name="q" class="form-control form-control-sm" maxlength="200"
                   value="${fn:escapeXml(filter.query)}" placeholder="Ví dụ: áo thun">
          </div>
          <div class="form-group">
            <span class="h6 text-uppercase text-black d-block">Giá (₫)</span>
            <div class="d-flex align-items-center">
              <input type="number" name="minPrice" class="form-control form-control-sm" min="0" step="1000"
                     value="${filter.minPrice}" placeholder="Từ" aria-label="Giá từ">
              <span class="mx-2">–</span>
              <input type="number" name="maxPrice" class="form-control form-control-sm" min="0" step="1000"
                     value="${filter.maxPrice}" placeholder="Đến" aria-label="Giá đến">
            </div>
          </div>
          <c:if test="${not empty colors}">
            <div class="form-group">
              <span class="h6 text-uppercase text-black d-block">Màu sắc</span>
              <label class="d-flex align-items-center mb-1">
                <input type="radio" name="color" value="" class="mr-2" ${empty filter.color ? 'checked' : ''}>
                <span class="text-black">Tất cả</span>
              </label>
              <c:forEach items="${colors}" var="col" varStatus="st">
                <label class="d-flex align-items-center mb-1" for="color_${st.index}">
                  <input type="radio" id="color_${st.index}" name="color" value="${fn:escapeXml(col)}" class="mr-2"
                         ${fn:toLowerCase(filter.color) == fn:toLowerCase(col) ? 'checked' : ''}>
                  <span class="text-black"><c:out value="${col}"/></span>
                </label>
              </c:forEach>
            </div>
          </c:if>
          <button type="submit" class="btn btn-primary btn-sm btn-block">Lọc</button>
          <a href="${ctx}/shop<c:if test="${not empty filter.category}">?category=${filter.category}</c:if>" class="btn btn-link btn-sm btn-block">Xóa bộ lọc</a>
        </form>
      </div>
    </div>
  </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
