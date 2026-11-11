/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.graphql.runtimewiring

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class GreetingController {

	@QueryMapping
	fun greeting(@Argument name: String): String {
		return "Hello, $name!"
	}

}

