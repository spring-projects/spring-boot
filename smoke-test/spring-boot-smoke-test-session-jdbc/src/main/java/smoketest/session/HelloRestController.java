/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.session;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloRestController {

	@GetMapping("/")
	String uid(HttpSession session) {
		return session.getId();
	}

}
