/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * Example {@link Controller @Controller} used with {@link WebMvcTest @WebMvcTest} tests.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
@RestController
public class ExampleController1 {

	@GetMapping("/one")
	public String one() {
		return "one";
	}

	@GetMapping("/error")
	public String error() {
		throw new ExampleException();
	}

	@GetMapping(path = "/html", produces = "text/html")
	public String html() {
		return "<html><body>Hello</body></html>";
	}

	@GetMapping("/formatting")
	public String formatting(WebRequest request) {
		Object formattingFails = new Object() {
			@Override
			public String toString() {
				throw new IllegalStateException("Formatting failed");
			}
		};
		request.setAttribute("attribute-1", formattingFails, RequestAttributes.SCOPE_SESSION);
		return "formatting";
	}

}
