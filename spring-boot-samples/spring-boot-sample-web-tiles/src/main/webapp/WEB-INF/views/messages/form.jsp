<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<spring:url value="/" var="createUrl"/>
<form:form id="messageForm" action="${createUrl}" method="post" modelAttribute="message" class="form-horizontal">
	<form:errors cssClass="text-warning" path="*" />
	<div class="form-group">
		<label for="summary" class="col-xs-2">Summary</label>
		<div class="col-xs-6">
			<form:input path="summary" class="form-control"/>
		</div>
		<div class="col-xs-4">
			<form:errors path="summary"/>
		</div>
	</div>
	<div class="form-group">
		<label for="text" class="col-xs-2">Text</label>
		<div class="col-xs-6">
			<form:textarea path="text" class="form-control" rows="4"/>
		</div>
		<div class="col-xs-4">
			<form:errors path="text"/>
		</div>
	</div>
	<div class="form-group">
		<input type="submit" value="Create" class="btn btn-primary"/>
	</div>
</form:form>
