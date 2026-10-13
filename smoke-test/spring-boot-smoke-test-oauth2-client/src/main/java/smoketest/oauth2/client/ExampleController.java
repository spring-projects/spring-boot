/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.oauth2.client;

import java.security.Principal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController {

	@RequestMapping("/")
	public String email(Principal principal) {
		return "Hello " + principal.getName();
	}

}
