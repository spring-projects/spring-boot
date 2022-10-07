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
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Tests for error page that permits access to all.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = { AbstractErrorPageTests.TestConfiguration.class,
				UnauthenticatedErrorPageTests.SecurityConfiguration.class, SampleWebSecureApplication.class },
		properties = { "server.error.include-message=always", "spring.security.user.name=username",
				"spring.security.user.password=password" })
class UnauthenticatedErrorPageTests extends AbstractUnauthenticatedErrorPageTests {

	UnauthenticatedErrorPageTests() {
		super("");
	}

	@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
			http.authorizeHttpRequests((requests) -> {
				requests.requestMatchers("/error").permitAll();
				requests.requestMatchers("/public/**").permitAll();
				requests.anyRequest().authenticated();
			});
			http.setSharedObject(SecurityContextRepository.class, new RequestAttributeSecurityContextRepository());
			http.httpBasic();
			return http.build();
		}

	}

}
