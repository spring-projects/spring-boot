/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.secure.webflux;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

	@GetMapping("/")
	public String welcome(Principal principal) {
		return "Hello " + principal.getName();
	}

}
