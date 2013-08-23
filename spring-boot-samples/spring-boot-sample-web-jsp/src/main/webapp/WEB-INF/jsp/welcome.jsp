<!DOCTYPE html>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<html lang="en">

<body>
	<spring:url value="/resources/text.txt" htmlEscape="true" var="springUrl" />
	${springUrl}
</body>

</html>
