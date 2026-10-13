/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.war;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

	@GetMapping("/")
	public String hello() {
		return "Hello World!";
	}

}
