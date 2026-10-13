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
 * Tests to ensure that the error page is accessible only to authorized users.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = { AbstractErrorPageTests.TestConfiguration.class, ErrorPageTests.SecurityConfiguration.class,
				SampleWebSecureApplication.class },
		properties = { "spring.web.error.include-message=always", "spring.security.user.name=username",
				"spring.security.user.password=password" })
class ErrorPageTests extends AbstractErrorPageTests {

	ErrorPageTests() {
		super("");
	}

	@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityFilterChain configure(HttpSecurity http) {
			http.authorizeHttpRequests((requests) -> {
				requests.requestMatchers("/public/**").permitAll();
				requests.anyRequest().fullyAuthenticated();
			});
			http.httpBasic(withDefaults());
			http.formLogin((form) -> form.loginPage("/login").permitAll());
			return http.build();
		}

	}

}
