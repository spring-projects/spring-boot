<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>


<!DOCTYPE html>
<html>
<head>
	<spring:url value="/css/bootstrap.min.css" var="bootstrapcss"/>
	<spring:url value="/css/custom.css" var="customcss"/>
	<title>Tiles Layout</title>
	<link rel="stylesheet" href="${bootstrapcss}"/>
	<link rel="stylesheet" href="${customcss}"/>
</head>
<body>
<div class="container">
	<div class="row">
		 <div class="col-xs-12">
			 <nav class="navbar navbar-default">
				 <div class="navbar-header">
					 <a class="navbar-brand" href="#">Tiles based layout</a>
					 <ul class="nav navbar-nav">
						 <li>
							 <spring:url var="messages" value="/"/>
							 <a href="${messages}">
								 Messages
							 </a>
						 </li>
					 </ul>
				 </div>
			 </nav>
		 </div>
	</div>
	<div class="row">
		<div class="col-xs-12">
			<h3><tiles:getAsString name="pagetitle"/></h3>
		</div>
	</div>
	<div class="row">
		<div class="col-xs-12">
		<tiles:insertAttribute name="body"/>
		</div>
	</div>
</div>
</body>
</html>

