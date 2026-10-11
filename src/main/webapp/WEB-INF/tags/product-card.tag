<%@ tag pageEncoding="UTF-8" body-content="empty" description="Ô sản phẩm ở trang cửa hàng (FR-07 bước 3)" %>
<%@ attribute name="product" required="true" type="com.clothingshop.dto.ProductSummary" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="f" uri="/WEB-INF/shop.tld" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<div class="block-4 text-center product-card border">
  <c:if test="${product.onSale}"><span class="badge badge-danger badge-sale">-${f:number(product.discountPercent)}%</span></c:if>
  <c:if test="${product.soldOut}"><span class="badge badge-secondary badge-soldout">Hết hàng</span></c:if>
  <figure class="block-4-image">
    <a href="${ctx}/product?id=${product.id}">
      <img src="${fn:escapeXml(f:imageUrl(ctx, product.imgUrl))}" alt="${fn:escapeXml(product.name)}" class="img-fluid">
    </a>
  </figure>
  <div class="block-4-text p-4">
    <h3><a href="${ctx}/product?id=${product.id}"><c:out value="${product.name}"/></a></h3>
    <p class="mb-0 text-muted small"><c:out value="${product.category.label}"/></p>
    <p class="text-primary font-weight-bold mb-0">
      <c:if test="${product.onSale}"><span class="price-old">${f:vnd(product.basePrice)}</span></c:if>
      ${f:vnd(product.currentPrice)}
    </p>
  </div>
</div>
