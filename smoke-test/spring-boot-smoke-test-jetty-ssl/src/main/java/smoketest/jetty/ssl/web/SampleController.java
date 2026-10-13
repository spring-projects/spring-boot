/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jetty.ssl.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {

	@GetMapping("/")
	public String helloWorld() {
		return "Hello World";
	}

}
