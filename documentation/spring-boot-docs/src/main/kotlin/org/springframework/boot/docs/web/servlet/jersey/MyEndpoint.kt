/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.jersey

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.springframework.stereotype.Component

@Component
@Path("/hello")
class MyEndpoint {

	@GET
	fun message(): String {
		return "Hello"
	}

}
