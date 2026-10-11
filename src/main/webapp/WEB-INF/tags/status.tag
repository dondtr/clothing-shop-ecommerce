<%@ tag pageEncoding="UTF-8" body-content="empty" description="Nhãn màu cho một trạng thái (đơn hàng, thanh toán, sự cố, trả hàng...)" %>
<%@ attribute name="value" required="true" type="java.lang.Object" description="enum có getLabel()" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:if test="${not empty value}"><span class="badge status-badge status-${fn:toLowerCase(value)}"><c:out value="${value.label}"/></span></c:if>
