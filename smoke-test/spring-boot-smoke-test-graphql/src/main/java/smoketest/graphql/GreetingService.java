/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.graphql;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class GreetingService {

	@PreAuthorize("hasRole('ADMIN')")
	public String greet(String name) {
		return "Hello, " + name + "!";
	}

}
