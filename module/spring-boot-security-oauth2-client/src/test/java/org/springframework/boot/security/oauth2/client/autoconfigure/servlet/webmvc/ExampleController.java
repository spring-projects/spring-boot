/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure.servlet.webmvc;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example {@link Controller @Controller} used with {@link WebMvcTest @WebMvcTest} tests.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
@RestController
public class ExampleController {

	@GetMapping("/one")
	public String one() {
		return "one";
	}

}
