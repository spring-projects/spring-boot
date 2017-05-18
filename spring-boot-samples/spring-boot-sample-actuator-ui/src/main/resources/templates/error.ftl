<#import "/spring.ftl" as spring />
<!DOCTYPE html>
<html>
<head>
<title>Error</title>
<#assign home><@spring.url relativeUrl="/"/></#assign>
<#assign bootstrap><@spring.url relativeUrl="/css/bootstrap.min.css"/></#assign>
<link rel="stylesheet" href="${bootstrap}" />
</head>
<body>
	<div class="container">
		<div class="navbar">
			<div class="navbar-inner">
				<a class="brand" href="http://freemarker.org/"> FreeMarker -
					Plain </a>
				<ul class="nav">
					<li><a href="${home}"> Home </a></li>
				</ul>
			</div>
		</div>
		<h1>Error Page</h1>
		<div id="created">${timestamp?datetime}</div>
		<div>
			There was an unexpected error (type=${error}, status=${status}).
		</div>
		<div>${message}</div>
		<div>
			Please contact the operator with the above information.
		</div>
	</div>
</body>
</html>
