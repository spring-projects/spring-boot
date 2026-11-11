/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.web.graphql.runtimewiring;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class GreetingController {

	@QueryMapping
	public String greeting(@Argument String name) {
		return "Hello, " + name + "!";
	}

}
