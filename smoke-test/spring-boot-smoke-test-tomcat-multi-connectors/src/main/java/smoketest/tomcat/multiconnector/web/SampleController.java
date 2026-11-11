/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.tomcat.multiconnector.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {

	@GetMapping("/hello")
	public String helloWorld() {
		return "hello";
	}

}
