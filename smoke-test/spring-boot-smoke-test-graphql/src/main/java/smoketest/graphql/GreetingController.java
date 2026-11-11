/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class GreetingController {

	private final GreetingService greetingService;

	public GreetingController(GreetingService greetingService) {
		this.greetingService = greetingService;
	}

	@QueryMapping
	public String greeting(@Argument String name) {
		return this.greetingService.greet(name);
	}

}
