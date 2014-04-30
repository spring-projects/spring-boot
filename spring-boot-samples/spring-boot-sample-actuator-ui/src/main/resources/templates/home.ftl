<#import "/spring.ftl" as spring />
<!DOCTYPE html>
<html>
<head>
<title>${title}</title>
<#assign home><@spring.url relativeUrl="/"/></#assign>
<#assign bootstrap><@spring.url relativeUrl="/css/bootstrap.min.css"/></#assign>
<link rel="stylesheet" href="${bootstrap}" />
</head>
<body>
	<div class="container">
		<div class="navbar">
			<div class="navbar-inner">
				<a class="brand" href="http://www.thymeleaf.org"> Freemarker -
					Plain </a>
				<ul class="nav">
					<li><a href="${home}"> Home </a></li>
				</ul>
			</div>
		</div>
		<h1>${title}</h1>
		<div>${message}</div>
		<div id="created">${date?datetime}</div>
	</div>
</body>
</html>
