/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.servlet.springmvc

import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Suppress("UNUSED_PARAMETER")
@Component
class MyUserHandler {

	fun getUser(request: ServerRequest?): ServerResponse {
		/**/ return ServerResponse.ok().build()
	}

	fun getUserCustomers(request: ServerRequest?): ServerResponse {
		/**/ return ServerResponse.ok().build()
	}

	fun deleteUser(request: ServerRequest?): ServerResponse {
		/**/ return ServerResponse.ok().build()
	}

}

