/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.secure;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Tests for error page that permits access to all.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = { AbstractErrorPageTests.TestConfiguration.class,
				UnauthenticatedErrorPageTests.SecurityConfiguration.class, SampleWebSecureApplication.class },
		properties = { "spring.web.error.include-message=always", "spring.security.user.name=username",
				"spring.security.user.password=password" })
class UnauthenticatedErrorPageTests extends AbstractUnauthenticatedErrorPageTests {

	UnauthenticatedErrorPageTests() {
		super("");
	}

	@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
			http.authorizeHttpRequests((requests) -> {
				requests.requestMatchers("/error").permitAll();
				requests.requestMatchers("/public/**").permitAll();
				requests.anyRequest().authenticated();
			});
			http.httpBasic(withDefaults());
			return http.build();
		}

	}

}
