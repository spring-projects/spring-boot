<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<!DOCTYPE html>
<html>
<head>
        <title> Spring Boot with Apache Tiles</title>
</head>
<body>
        <div class="container" style="margin:50px">
                <tiles:insertAttribute name="header"/>
                <tiles:insertAttribute name="body"/>
                <tiles:insertAttribute name="footer"/>
        </div>
</body>
</html>