/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.oauth2.resource;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController {

	@GetMapping("/")
	public String index(@AuthenticationPrincipal Jwt jwt) {
		return String.format("Hello, %s!", jwt.getSubject());
	}

}
