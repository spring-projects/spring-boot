/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.session;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;

@RestController
public class HelloRestController {

	@GetMapping("/")
	String sessionId(WebSession session) {
		return session.getId();
	}

}
