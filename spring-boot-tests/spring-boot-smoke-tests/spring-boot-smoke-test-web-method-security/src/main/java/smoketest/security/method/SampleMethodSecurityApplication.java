/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.security.method;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * SampleMethodSecurityApplication class.
 */
@SpringBootApplication
@EnableMethodSecurity(securedEnabled = true)
public class SampleMethodSecurityApplication implements WebMvcConfigurer {

	/**
	 * Adds view controllers for the login and access pages.
	 * @param registry the ViewControllerRegistry to register the view controllers with
	 */
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/login").setViewName("login");
		registry.addViewController("/access").setViewName("access");
	}

	/**
	 * The main method is the entry point of the application. It initializes and runs the
	 * SpringApplicationBuilder to start the SampleMethodSecurityApplication.
	 * @param args the command line arguments passed to the application
	 */
	public static void main(String[] args) {
		new SpringApplicationBuilder(SampleMethodSecurityApplication.class).run(args);
	}

	/**
	 * AuthenticationSecurity class.
	 */
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@Configuration(proxyBeanMethods = false)
	protected static class AuthenticationSecurity {

		/**
		 * Creates an instance of InMemoryUserDetailsManager with two users: admin and
		 * user. The admin user has the roles ADMIN, USER, and ACTUATOR, while the user
		 * user has the role USER.
		 * @return the InMemoryUserDetailsManager instance
		 * @deprecated This method uses the deprecated DefaultPasswordEncoder. It is
		 * recommended to use a more secure password encoder.
		 */
		@SuppressWarnings("deprecation")
		@Bean
		public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
			return new InMemoryUserDetailsManager(
					User.withDefaultPasswordEncoder()
						.username("admin")
						.password("admin")
						.roles("ADMIN", "USER", "ACTUATOR")
						.build(),
					User.withDefaultPasswordEncoder().username("user").password("user").roles("USER").build());
		}

	}

	/**
	 * ApplicationSecurity class.
	 */
	@Configuration(proxyBeanMethods = false)
	protected static class ApplicationSecurity {

		/**
		 * Configures the security filter chain for the application. Disables CSRF
		 * protection. Configures authorization rules for HTTP requests. Enables HTTP
		 * basic authentication. Configures form login and sets the login page. Configures
		 * exception handling and sets the access denied page.
		 * @param http the HttpSecurity object to configure
		 * @return the configured SecurityFilterChain
		 * @throws Exception if an error occurs during configuration
		 */
		@Bean
		SecurityFilterChain configure(HttpSecurity http) throws Exception {
			http.csrf((csrf) -> csrf.disable());
			http.authorizeHttpRequests((requests) -> {
				requests.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll();
				requests.anyRequest().fullyAuthenticated();
			});
			http.httpBasic(withDefaults());
			http.formLogin((form) -> form.loginPage("/login").permitAll());
			http.exceptionHandling((exceptions) -> exceptions.accessDeniedPage("/access"));
			return http.build();
		}

	}

	/**
	 * ActuatorSecurity class.
	 */
	@Configuration(proxyBeanMethods = false)
	@Order(1)
	protected static class ActuatorSecurity {

		/**
		 * Configures the security filter chain for the Actuator endpoints.
		 * @param http the HttpSecurity object to configure
		 * @return the configured SecurityFilterChain object
		 * @throws Exception if an error occurs during configuration
		 */
		@Bean
		SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
			http.csrf((csrf) -> csrf.disable());
			http.securityMatcher(EndpointRequest.toAnyEndpoint());
			http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
			http.httpBasic(withDefaults());
			return http.build();
		}

	}

	/**
	 * HomeController class.
	 */
	@Controller
	protected static class HomeController {

		/**
		 * Retrieves the home page.
		 * @return the name of the home page
		 *
		 * @secured Requires the user to have the "ROLE_ADMIN" role in order to access
		 * this method.
		 */
		@GetMapping("/")
		@Secured("ROLE_ADMIN")
		public String home() {
			return "home";
		}

	}

}
