/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
