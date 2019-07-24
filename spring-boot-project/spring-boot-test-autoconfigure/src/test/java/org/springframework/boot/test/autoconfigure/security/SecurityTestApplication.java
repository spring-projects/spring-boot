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

package org.springframework.boot.test.autoconfigure.security;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcSecurityAutoConfiguration;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests application for {@link MockMvcSecurityAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@SpringBootApplication
public class SecurityTestApplication {

	@RestController
	static class MyController {

		@RequestMapping("/")
		@Secured("ROLE_USER")
		public String index() {
			return "Hello";
		}

	}

}
