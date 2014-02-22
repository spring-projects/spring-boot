<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core"   prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"    prefix="fmt" %>


<div class="pull-right">
	<spring:url value="/" var="messageform">
		<spring:param name="form" value="true"/>
	</spring:url>
	<a href="${messageform}">Create Message</a>
</div>
<table class="table table-bordered table-striped">
	<thead>
	<tr>
		<td>ID</td>
		<td>Created</td>
		<td>Summary</td>
	</tr>
	</thead>
	<tbody>
	<c:choose>
		<c:when test="${empty messages}">
			<tr>
				<td colspan="3">
					No messages
				</td>
			</tr>
		</c:when>
		<c:otherwise>
			<c:forEach var="message" items="${messages}">
				<tr>
					<td>${message.id}</td>
					<td>
						<fmt:formatDate value="${message.created.time}" />
					</td>
					<td>
						<spring:url var="messagedetail" value="/${message.id}"/>
						<a href="${messagedetail}">
								${message.summary}
						</a>
					</td>
				</tr>
			</c:forEach>
		</c:otherwise>
	</c:choose>
	</tbody>
</table>