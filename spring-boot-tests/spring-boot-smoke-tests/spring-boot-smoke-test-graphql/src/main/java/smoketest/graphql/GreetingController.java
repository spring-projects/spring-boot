/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GreetingController class.
 */
@Controller
public class GreetingController {

	private final GreetingService greetingService;

	/**
	 * Constructs a new GreetingController with the specified GreetingService.
	 * @param greetingService the GreetingService to be used by the controller
	 */
	public GreetingController(GreetingService greetingService) {
		this.greetingService = greetingService;
	}

	/**
	 * Retrieves a greeting message for the given name.
	 * @param name the name to greet
	 * @return the greeting message
	 */
	@QueryMapping
	public String greeting(@Argument String name) {
		return this.greetingService.greet(name);
	}

}
