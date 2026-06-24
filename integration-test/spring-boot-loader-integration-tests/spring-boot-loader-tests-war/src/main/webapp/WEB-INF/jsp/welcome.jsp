<!DOCTYPE html>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html lang="en">
<body>
	<c:url value="/" var="url"/>
	<spring:url value="/" htmlEscape="true" var="springUrl" />
	Spring URL: ${springUrl}
	JSTL URL: ${url}
</body>
</html>
