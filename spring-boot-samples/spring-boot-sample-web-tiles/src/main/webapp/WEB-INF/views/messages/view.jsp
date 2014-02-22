<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>


<c:if test="${not empty globalMessage}">
	<div class="alert alert-success">
		${globalMessage}
	</div>
</c:if>
<dl>
	<dt>ID</dt>
	<dd id="id">${message.id}</dd>
	<dt>Date</dt>
	<dd id="created"><fmt:formatDate value="${message.created.time}"/></dd>
	<dt>Summary</dt>
	<dd id="summary">
		${message.summary}
	</dd>
	<dt>Message</dt>
	<dd id="text">
		${message.text}
	</dd>
</dl>
