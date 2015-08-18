<% endpoints.each { endpoint ->
  if (endpoint.custom) { %>
include::{docs}/${endpoint.custom}[]
<% } else { %>
=== Link: ${endpoint.title}

Example curl request:
include::{generated}${endpoint.path}/curl-request.adoc[]

Example HTTP request:
include::{generated}${endpoint.path}/http-request.adoc[]

Example HTTP response:
include::{generated}${endpoint.path}/http-response.adoc[]
<% }
} %>