<%@ tag pageEncoding="UTF-8" body-content="scriptless" description="Một ô nhập có nhãn, gợi ý và thông báo lỗi; có nội dung bên trong (ví dụ select) thì dùng nội dung đó thay cho input" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="value" %>
<%@ attribute name="type" %>
<%@ attribute name="required" type="java.lang.Boolean" %>
<%@ attribute name="maxlength" %>
<%@ attribute name="min" %>
<%@ attribute name="max" %>
<%@ attribute name="step" %>
<%@ attribute name="placeholder" %>
<%@ attribute name="autocomplete" %>
<%@ attribute name="hint" %>
<%@ attribute name="error" %>
<%@ attribute name="cssClass" description="lớp thêm cho khung, ví dụ col-md-6" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:doBody var="body"/>
<div class="form-group ${cssClass}">
  <label for="${name}" class="text-black ${required ? 'form-required' : ''}"><c:out value="${label}"/></label>
  <c:choose>
    <c:when test="${not empty fn:trim(body)}">${body}</c:when>
    <c:otherwise>
      <input type="${empty type ? 'text' : type}" class="form-control ${not empty error ? 'is-invalid' : ''}" id="${name}" name="${name}"
             value="${fn:escapeXml(value)}" ${required ? 'required' : ''}
             <c:if test="${not empty maxlength}">maxlength="${maxlength}"</c:if>
             <c:if test="${not empty min}">min="${min}"</c:if>
             <c:if test="${not empty max}">max="${max}"</c:if>
             <c:if test="${not empty step}">step="${step}"</c:if>
             <c:if test="${not empty placeholder}">placeholder="${fn:escapeXml(placeholder)}"</c:if>
             <c:if test="${not empty autocomplete}">autocomplete="${autocomplete}"</c:if>>
    </c:otherwise>
  </c:choose>
  <c:choose>
    <c:when test="${not empty error}"><div class="invalid-feedback d-block"><c:out value="${error}"/></div></c:when>
    <c:when test="${not empty hint}"><small class="text-muted"><c:out value="${hint}"/></small></c:when>
  </c:choose>
</div>
