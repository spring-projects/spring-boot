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

package smoketest.web.secure;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Tests to ensure that the error page with a custom servlet path is accessible only to
 * authorized users.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = { AbstractErrorPageTests.TestConfiguration.class,
				CustomServletPathErrorPageTests.SecurityConfiguration.class, SampleWebSecureApplication.class },
		properties = { "server.error.include-message=always", "spring.security.user.name=username",
				"spring.security.user.password=password", "spring.mvc.servlet.path=/custom/servlet/path" })
class CustomServletPathErrorPageTests extends AbstractErrorPageTests {

	CustomServletPathErrorPageTests() {
		super("/custom/servlet/path");
	}

	@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityFilterChain configure(HttpSecurity http) throws Exception {
			http.authorizeRequests((requests) -> {
				requests.antMatchers("/custom/servlet/path/public/**").permitAll();
				requests.anyRequest().fullyAuthenticated();
				requests.filterSecurityInterceptorOncePerRequest(true);
			});
			http.httpBasic();
			http.formLogin((form) -> form.loginPage("/custom/servlet/path/login").permitAll());
			return http.build();
		}

	}

}
