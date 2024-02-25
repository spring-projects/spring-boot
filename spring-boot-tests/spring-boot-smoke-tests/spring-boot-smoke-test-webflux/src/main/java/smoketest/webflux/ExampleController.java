/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.webflux;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ExampleController class.
 */
@RestController
public class ExampleController {

	/**
	 * This method is used to handle POST requests to the root path ("/"). It consumes
	 * JSON data and does not consume XML data. It produces plain text as the response. It
	 * requires the "X-Custom" header to have a value of "Foo". It requires the "a"
	 * parameter to not have a value of "alpha".
	 * @return The string "Hello World" as the response.
	 */
	@PostMapping(path = "/", consumes = { MediaType.APPLICATION_JSON_VALUE, "!application/xml" },
			produces = MediaType.TEXT_PLAIN_VALUE, headers = "X-Custom=Foo", params = "a!=alpha")
	public String example() {
		return "Hello World";
	}

}
